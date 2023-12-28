package in.pratanumandal.brainfuck.gui.component.terminal;

import org.fxmisc.richtext.CodeArea;

public class Style {

    private String text;
    private int start;
    private StyleClass styleClass;

    public Style(String text, int start, StyleClass styleClass) {
        this.text = text;
        this.start = start;
        this.styleClass = styleClass;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return start + Style.getVirtualLength(text);
    }

    public StyleClass getStyleClass() {
        return styleClass;
    }

    private static int getVirtualLength(String text) {
        CodeArea codePad = new CodeArea(text);
        return codePad.getLength();
    }

}
