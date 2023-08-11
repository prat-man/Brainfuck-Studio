package in.pratanumandal.brainfuck.engine.processor.translator;

import java.io.BufferedWriter;
import java.io.IOException;

public class TranslationWriter implements AutoCloseable {

    private final BufferedWriter writer;
    private int indentation;

    public TranslationWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void write(String text) throws IOException {
        this.writer.write(" ".repeat(indentation * 4) + text);
    }

    public void writeLine() throws IOException {
        this.write("\n");
    }

    public void writeLine(String text) throws IOException {
        this.write(text + "\n");
    }

    public void updateIndentation(int delta) {
        this.indentation += delta;
    }

    public void flush() throws IOException {
        this.writer.flush();
    }

    public void close() throws IOException {
        this.writer.close();
    }

}
