package in.pratanumandal.brainfuck.gui;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FXTerminal extends TextArea {

    private String existingText;
    private String readBuffer;

    private final AtomicBoolean readLock;
    private final AtomicBoolean autoScroll;

    private final StringBuilder writeBuffer;
    private final ScheduledFuture<?> future;

    private final Object flushLock;

    public FXTerminal() {
        this("");
    }

    public FXTerminal(String text) {
        this.existingText = this.sanitizeText(text);
        super.setText(this.existingText);

        this.readBuffer = "";

        this.readLock = new AtomicBoolean(false);
        this.autoScroll = new AtomicBoolean(true);

        this.flushLock = new Object();

        this.getStyleClass().add("terminal");

        // disable changing text
        this.setTextFormatter(new TextFormatter<String>((TextFormatter.Change c) -> {
            String proposed;
            try {
                proposed = c.getControlNewText();
            }
            catch (Exception e) {
                proposed = "";
            }
            boolean readLock = this.readLock.get();
            if (!readLock && proposed.equals(this.existingText)) {
                return c;
            } else if (readLock && proposed.startsWith(this.existingText)) {
                return c;
            } else {
                return null;
            }
        }));

        // disable lock when newline is in input
        this.textProperty().addListener((obs, oldVal, newVal) -> {
            if (this.readLock.get() && newVal.substring(this.existingText.length()).contains("\n")) {
                synchronized (this.readLock) {
                    this.readLock.set(false);
                    this.readLock.notifyAll();
                }
            }
        });

        // correct mouse click at end of text
        this.setOnMouseClicked(event -> {
            if (this.getCaretPosition() == this.getLength()) {
                this.positionCaret(0);
                this.positionCaret(this.getLength());
            }
        });

        this.writeBuffer = new StringBuilder();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        this.future = scheduler.scheduleWithFixedDelay(() -> {
            synchronized (this.writeBuffer) {
                if (this.writeBuffer.length() > 0) {
                    String newText = this.writeBuffer.toString();
                    this.writeBuffer.setLength(0);

                    this.existingText += this.sanitizeText(newText);

                    if (this.autoScroll.get()) {
                        Platform.runLater(() -> {
                            // set the text
                            this.setText(this.existingText);

                            // reset properties
                            this.setScrollTop(Double.MAX_VALUE);
                            this.positionCaret(this.getLength());
                        });
                    } else {
                        Platform.runLater(() -> {
                            // get current properties
                            double scrollLeft = this.getScrollLeft();
                            double scrollTop = this.getScrollTop();
                            int caretPos = this.getCaretPosition();
                            IndexRange selection = this.getSelection();

                            // set the text
                            this.setText(this.existingText);

                            // reset properties
                            this.setScrollLeft(scrollLeft);
                            this.setScrollTop(scrollTop);
                            this.positionCaret(caretPos);
                            this.selectRange(selection.getStart(), selection.getEnd());
                        });
                    }
                }
                synchronized (this.flushLock) {
                    this.flushLock.notifyAll();
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        ContextMenu menu = new ContextMenu();
        menu.onShownProperty().addListener((obs, oldVal, newVal) -> {
            IndexRange selection = this.getSelection();
            this.selectRange(selection.getStart(), selection.getEnd());
        });
        this.setContextMenu(menu);

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(this.getSelectedText());
            clipboard.setContent(content);
        });
        menu.getItems().add(copyItem);

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            this.appendText(clipboard.getString());
        });
        menu.getItems().add(pasteItem);

        MenuItem clearItem = new MenuItem("Clear");
        clearItem.setOnAction(event -> this.clear());
        menu.getItems().add(clearItem);

        menu.getItems().add(new SeparatorMenuItem());

        CheckMenuItem scrollItem = new CheckMenuItem("Scroll on output");
        scrollItem.setSelected(this.autoScroll.get());
        scrollItem.selectedProperty().addListener((obs, oldVal, newVal) -> this.autoScroll.set(newVal));
        menu.getItems().add(scrollItem);
    }

    public Character readChar() {
        if (this.readBuffer.isEmpty()) {
            if (this.autoScroll.get()) {
                this.setScrollTop(Double.MAX_VALUE);
                this.positionCaret(this.getLength());
            }

            Platform.runLater(() -> this.requestFocus());

            synchronized (this.readLock) {
                try {
                    this.readLock.set(true);
                    this.readLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.readBuffer = this.getText().substring(this.existingText.length());
            this.existingText = this.getText();
        }

        if (this.readBuffer.isEmpty()) return null;

        Character character = this.readBuffer.charAt(0);
        this.readBuffer = this.readBuffer.substring(1);

        return character;
    }

    public void write(String text) {
        synchronized (this.writeBuffer) {
            this.writeBuffer.append(text);
        }
    }

    public void reset() {
        this.clear();
        this.readBuffer = "";
    }

    public void clear() {
        this.existingText = "";
        this.setText(this.existingText);
    }

    public void flush() {
        synchronized (this.flushLock) {
            try {
                this.flushLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void release() {
        synchronized (this.readLock) {
            this.readLock.set(false);
            this.readLock.notifyAll();
        }
    }

    public void destroy() {
        this.future.cancel(true);
    }

    private String sanitizeText(String text) {
        return text.replace("\r", "")
                .replaceAll("[\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}&&[^\\s]]", "\uFFFD");
    }

}
