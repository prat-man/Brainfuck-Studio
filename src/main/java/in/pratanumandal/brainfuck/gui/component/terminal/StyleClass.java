package in.pratanumandal.brainfuck.gui.component.terminal;

public enum StyleClass {

    INPUT ("input"),
    OUTPUT("output"),
    MESSAGE ("message"),
    ERROR ("error");

    private String value;

    StyleClass(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
