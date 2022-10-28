package in.pratanumandal.brainfuck.gui;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FXTerminal extends CodeArea {

    private String existingText;
    private String readBuffer;

    private final AtomicBoolean readLock;
    private final AtomicBoolean autoScroll;

    private final StringBuilder writeBuffer;
    private final SortedMap<Integer, Integer> messageMap;
    private final ScheduledFuture<?> future;

    private final Object flushLock;
    private final Object writeLock;

    public FXTerminal() {
        this("");
    }

    public FXTerminal(String text) {
        this.existingText = this.sanitizeText(text);
        super.replaceText(0, this.getLength(), this.existingText);

        this.readBuffer = "";

        this.readLock = new AtomicBoolean(false);
        this.autoScroll = new AtomicBoolean(true);

        this.flushLock = new Object();
        this.writeLock = new Object();

        this.getStyleClass().add("terminal");

        // disable changing text
        this.textProperty().addListener((obs, existing, proposed) -> {
            boolean readLock = this.readLock.get();
            if (!readLock && proposed.equals(this.existingText)) {
                // do nothing
            } else if (readLock && proposed.startsWith(this.existingText)) {
                // do nothing
            } else {
                // disallow change
                this.replaceText(0, this.getLength(), this.existingText);
            }
        });

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
        /*this.setOnMouseClicked(event -> {
            if (this.getCaretPosition() == this.getLength()) {
                this.moveTo(0);
                this.requestFollowCaret();
            }
        });*/

        this.writeBuffer = new StringBuilder();

        this.messageMap = new TreeMap<>();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        this.future = scheduler.scheduleWithFixedDelay(() -> {
            synchronized (this.writeLock) {
                if (this.writeBuffer.length() > 0) {
                    String newText = this.writeBuffer.toString();
                    this.writeBuffer.setLength(0);

                    this.existingText += newText;

                    if (this.autoScroll.get()) {
                        Platform.runLater(() -> {
                            // set the text
                            this.replaceText(0, this.getLength(), this.existingText);

                            // set highlighting
                            this.setStyleSpans(0, this.computeHighlighting());

                            // reset properties
                            /*this.setScrollTop(Double.MAX_VALUE);
                            this.positionCaret(this.getLength());*/
                            this.moveTo(this.getLength());
                            this.requestFollowCaret();
                        });
                    } else {
                        Platform.runLater(() -> {
                            // get current properties
                            double scrollLeft = this.getEstimatedScrollX();
                            double scrollTop = this.getEstimatedScrollY();
                            int caretPos = this.getCaretPosition();
                            IndexRange selection = this.getSelection();

                            // set the text
                            this.replaceText(0, this.getLength(), this.existingText);

                            // set highlighting
                            this.setStyleSpans(0, this.computeHighlighting());

                            // reset properties
                            this.scrollXToPixel(scrollLeft);
                            this.scrollYToPixel(scrollTop);
                            this.moveTo(caretPos);
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

    public int getVirutalLength() {
        return this.existingText.length() + this.writeBuffer.length();
    }

    public Character readChar() {
        if (this.readBuffer.isEmpty()) {
            if (this.autoScroll.get()) {
                this.moveTo(this.getLength());
                this.requestFollowCaret();
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
        String sanitizedText = this.sanitizeText(text);
        synchronized (this.writeLock) {
            this.writeBuffer.append(sanitizedText);
        }
    }

    public void writeMessage(String text) {
        String sanitizedText = this.sanitizeText(text);
        synchronized (this.writeLock) {
            this.messageMap.put(this.getVirutalLength(), sanitizedText.length());
            this.writeBuffer.append(sanitizedText);
        }
    }

    public void reset() {
        this.clear();
        this.readBuffer = "";
    }

    public void clear() {
        this.existingText = "";
        Platform.runLater(() -> this.replaceText(0, this.getLength(), this.existingText));
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

    private StyleSpans<Collection<String>> computeHighlighting() {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        int lastKwEnd = 0;
        for (Map.Entry<Integer, Integer> entry : messageMap.entrySet()) {
            int start = entry.getKey();
            int length = entry.getValue();

            // ignore styles for text that has not been actually written yet
            if (start > this.getLength()) break;
            if (start + length > this.getLength()) {
                length = this.getLength() - start;
            }

            spansBuilder.add(Collections.singleton("plain-text"), start - lastKwEnd);
            spansBuilder.add(Collections.singleton("message"), length);
            lastKwEnd = start + length;
        }

        if (lastKwEnd < this.getLength()) {
            spansBuilder.add(Collections.singleton("plain-text"), this.getLength() - lastKwEnd);
        }

        return spansBuilder.create();
    }

}
