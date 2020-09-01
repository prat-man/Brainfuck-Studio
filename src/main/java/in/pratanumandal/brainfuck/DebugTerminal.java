package in.pratanumandal.brainfuck;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;

public class DebugTerminal extends TextArea {

    private String existingText;

    private String readBuffer;

    private final Object readLock;

    public DebugTerminal() {
        this(new String());
    }

    public DebugTerminal(String text) {
        super(text);

        this.existingText = text;

        this.readBuffer = new String();

        this.readLock = new Object();

        this.setEditable(false);

        this.getStyleClass().add("debug-terminal");

        this.setTextFormatter(new TextFormatter<String>((TextFormatter.Change c) -> {
            String proposed = c.getControlNewText();
            if (proposed.startsWith(this.existingText)) {
                return c;
            } else {
                return null;
            }
        }));

        this.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                synchronized (this.readLock) {
                    this.readLock.notify();
                }
            }
        });
    }

    public Character readChar() {
        if (this.readBuffer.isEmpty()) {
            this.setEditable(true);
            this.positionCaret(this.getLength());

            synchronized (this.readLock) {
                try {
                    this.readLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.setEditable(false);

            this.readBuffer = this.getText().substring(this.existingText.length());
            this.existingText = this.getText();
        }

        if (this.readBuffer.isEmpty()) return null;

        Character character = this.readBuffer.charAt(0);
        this.readBuffer = this.readBuffer.substring(1);

        return character;
    }

    public void write(String text) {
        this.existingText += text;
        this.setText(this.existingText);
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
            this.readLock.notifyAll();
        }
    }

}
