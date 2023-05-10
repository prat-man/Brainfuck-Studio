package in.pratanumandal.brainfuck.engine;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class Memory {

    private SimpleBooleanProperty current;
    private SimpleIntegerProperty address;
    private SimpleIntegerProperty data;

    public Memory(Integer address) {
        this.current = new SimpleBooleanProperty(false);
        this.address = new SimpleIntegerProperty(address);
        this.data = new SimpleIntegerProperty(0);
    }

    public boolean isCurrent() {
        return current.get();
    }

    public SimpleBooleanProperty currentProperty() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current.set(current);
    }

    public SimpleIntegerProperty addressProperty() {
        return address;
    }

    public void setAddress(int address) {
        this.address.set(address);
    }

    public SimpleIntegerProperty dataProperty() {
        return data;
    }

    public void setData(int data) {
        this.data.set(data);
    }

    public Character getCharacter() {
        return Character.valueOf((char) data.intValue());
    }

}
