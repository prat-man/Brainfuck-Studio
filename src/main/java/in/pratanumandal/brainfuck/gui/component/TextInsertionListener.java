package in.pratanumandal.brainfuck.gui.component;

@FunctionalInterface
public interface TextInsertionListener {

    void codeInserted(int start, int end, String text);

}
