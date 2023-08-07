package in.pratanumandal.brainfuck.engine;

import in.pratanumandal.brainfuck.common.CharacterUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;

public class Memory {

    private SimpleBooleanProperty current;
    private SimpleIntegerProperty address;
    private SimpleLongProperty data;

    public Memory(Integer address) {
        this.current = new SimpleBooleanProperty(false);
        this.address = new SimpleIntegerProperty(address);
        this.data = new SimpleLongProperty(0);
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

    public SimpleLongProperty dataProperty() {
        return data;
    }

    public void setData(long data) {
        this.data.set(data);
    }

    public String getSymbol() {
        return CharacterUtils.getSymbol(data.get());
    }

}
