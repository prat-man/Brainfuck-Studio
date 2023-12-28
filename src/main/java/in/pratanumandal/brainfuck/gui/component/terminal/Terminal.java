package in.pratanumandal.brainfuck.gui.component.terminal;

import in.pratanumandal.brainfuck.common.SortedLinkedList;
import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.fxmisc.richtext.Caret;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.undo.UndoManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Terminal extends CodeArea {

    private String existingText;
    private String readBuffer;

    private Style inputStyle;

    private final AtomicBoolean readLock;
    private final AtomicBoolean autoScroll;

    private final StringBuilder writeBuffer;
    private final List<Style> styleList;
    private final ScheduledFuture<?> future;

    private final Object flushLock;
    private final Object writeLock;
    private StyleClass currentStyleClass;

    public Terminal() {
        this("");
    }

    public Terminal(String text) {
        this.existingText = text;
        super.replaceText(0, this.getLength(), this.existingText);

        this.readBuffer = "";

        this.readLock = new AtomicBoolean(false);
        this.autoScroll = new AtomicBoolean(true);

        this.flushLock = new Object();
        this.writeLock = new Object();

        this.getStyleClass().add("terminal");

        this.setShowCaret(Caret.CaretVisibility.ON);
        this.setEditable(false);

        // disable changing text
        this.caretPositionProperty().addListener((obs, oldVal, newVal) -> this.updateEditable());
        this.selectionProperty().addListener((obs, oldVal, newVal) -> this.updateEditable());

        // disable lock when newline is in input
        this.textProperty().addListener((obs, oldVal, newVal) -> {
            if (this.readLock.get()) {
                if (newVal.substring(this.existingText.length()).contains("\n")) {
                    this.release();
                    this.inputStyle = null;

                    // forget undo history
                    this.forgetUndoHistory();
                }
                else {
                    if (this.inputStyle == null) {
                        this.inputStyle = this.setStyle("", StyleClass.INPUT);
                    }

                    // compute new length
                    this.inputStyle.setText(this.getText().substring(this.inputStyle.getStart()));

                    // update highlighting
                    this.updateHighlighting();
                }
            }
        });

        this.writeBuffer = new StringBuilder();
        this.styleList = new SortedLinkedList<>(Comparator.comparingInt(Style::getStart));

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

                            // update highlighting
                            this.updateHighlighting();

                            // reset properties
                            this.moveTo(this.getLength());
                            this.requestFollowCaret();

                            // forget undo history
                            this.forgetUndoHistory();
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

                            // update highlighting
                            this.updateHighlighting();

                            // reset properties
                            this.scrollXToPixel(scrollLeft);
                            this.scrollYToPixel(scrollTop);
                            this.moveTo(caretPos);
                            this.selectRange(selection.getStart(), selection.getEnd());

                            // forget undo history
                            this.forgetUndoHistory();
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

    @Override
    public void deleteText(int start, int end) {
        boolean readLock = this.readLock.get();
        if (readLock &&
                start >= this.existingText.length() &&
                end >= this.existingText.length()) {
            super.deleteText(start, end);
        }
    }

    public void updateEditable() {
        IndexRange selection = this.getSelection();
        int caretPosition = this.getCaretPosition();
        boolean readLock = this.readLock.get();
        boolean editable = false;

        if (readLock) {
            if (selection.getLength() > 0) {
                if (selection.getStart() >= this.existingText.length() &&
                        selection.getEnd() >= this.existingText.length()) {
                    editable = true;
                }
            }
            else if (caretPosition >= this.existingText.length()) {
                editable = true;
            }
        }

        boolean finalEditable = editable;
        Platform.runLater(() -> this.setEditable(finalEditable));
    }

    public Character readChar() {
        if (this.readBuffer.isEmpty()) {
            if (this.autoScroll.get()) {
                Platform.runLater(() -> {
                    this.moveTo(this.getLength());
                    this.requestFollowCaret();
                });
            }

            Platform.runLater(() -> this.requestFocus());

            synchronized (this.readLock) {
                try {
                    this.readLock.set(true);
                    this.updateEditable();
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
        synchronized (this.writeLock) {
            this.setStyle(text, StyleClass.OUTPUT);
            this.writeBuffer.append(text);
        }
    }

    public void writeMessage(String text) {
        synchronized (this.writeLock) {
            this.setStyle(text, StyleClass.MESSAGE);
            this.writeBuffer.append(text);
        }
    }

    public void writeError(String text) {
        synchronized (this.writeLock) {
            this.setStyle(text, StyleClass.ERROR);
            this.writeBuffer.append(text);
        }
    }

    public void forgetUndoHistory() {
        UndoManager undoManager = this.getUndoManager();
        undoManager.forgetHistory();
    }

    public void reset() {
        this.readBuffer = "";
        this.inputStyle = null;
        this.currentStyleClass = StyleClass.OUTPUT;
        this.clear();
    }

    public void clear() {
        this.existingText = "";

        synchronized (this.styleList) {
            this.styleList.clear();
        }
        Platform.runLater(() -> this.updateHighlighting());

        if (this.inputStyle != null) {
            this.inputStyle = this.setStyle("", StyleClass.INPUT);
        }

        Platform.runLater(() -> {
            this.replaceText(0, this.getLength(), this.existingText);
            this.forgetUndoHistory();
        });
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

    private void updateHighlighting() {
        if (this.styleList.isEmpty()) {
            this.clearStyle(0, this.getLength());
        }
        else {
            this.setStyleSpans(0, this.computeHighlighting());
        }
    }

    private StyleSpans<Collection<String>> computeHighlighting() {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        synchronized (this.styleList) {
            for (Style style : this.styleList) {
                int length = Math.min(style.getEnd(), this.getLength()) - style.getStart();
                spansBuilder.add(Collections.singleton(style.getStyleClass().toString()), length);
            }
        }

        return spansBuilder.create();
    }

    private Style setStyle(String text, StyleClass styleClass) {
        Style style;

        if (this.currentStyleClass == styleClass) {
            if (this.styleList.isEmpty()) {
                style = new Style(text, 0, this.currentStyleClass);
                this.styleList.add(style);
            }
            else {
                style = this.styleList.get(this.styleList.size() - 1);
                style.setText(style.getText() + text);
            }
        }
        else {
            int start = 0;
            if (!this.styleList.isEmpty()) {
                Style lastStyle = this.styleList.get(this.styleList.size() - 1);
                start = lastStyle.getEnd();
            }

            this.currentStyleClass = styleClass;

            style = new Style(text, start, this.currentStyleClass);
            this.styleList.add(style);
        }

        return style;
    }

}
