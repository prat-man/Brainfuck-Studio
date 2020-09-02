package in.pratanumandal.brainfuck.gui;

import javafx.application.Platform;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;

import java.util.concurrent.atomic.AtomicBoolean;

public class DebugTerminal extends TextArea {

    private String existingText;

    private String readBuffer;

    private final AtomicBoolean readLock;

    public DebugTerminal() {
        this(new String());
    }

    public DebugTerminal(String text) {
        super(text);

        this.existingText = text;

        this.readBuffer = new String();

        this.readLock = new AtomicBoolean(false);

        this.getStyleClass().add("debug-terminal");

        // disable changing text
        this.setTextFormatter(new TextFormatter<String>((TextFormatter.Change c) -> {
            String proposed = c.getControlNewText();
            boolean readLock = this.readLock.get();
            if (!readLock && proposed.equals(this.existingText)) {
                return c;
            } else if (readLock && proposed.startsWith(this.existingText)) {
                return c;
            } else {
                return null;
            }
        }));

        // disable lock when enter key is pressed
        this.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
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
    }

    public Character readChar() {
        if (this.readBuffer.isEmpty()) {
            this.positionCaret(this.getLength());
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
        IndexRange range = this.getSelection();
        this.existingText += text;
        this.setText(this.existingText);
        this.selectRange(range.getStart(), range.getEnd());
    }

    public void clear() {
        this.existingText = new String();
        this.setText(this.existingText);
    }

    public void flush() {
        this.readBuffer = new String();
    }

    public void release() {
        synchronized (this.readLock) {
            this.readLock.set(false);
            this.readLock.notifyAll();
        }
    }

}
