package in.pratanumandal.brainfuck.engine.processor.interpreter;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.engine.processor.Processor;
import in.pratanumandal.brainfuck.gui.component.TabData;

public abstract class Interpreter extends Processor {

    protected Interpreter(TabData tabData) {
        super(tabData);
    }

    public static Interpreter getInterpreter(TabData tabData) {
        switch (Configuration.getCellSize()) {
            case 8: return new Interpreter8(tabData);
            case 16: return new Interpreter16(tabData);
            case 32: return new Interpreter32(tabData);
            default: return null;
        }
    }

}
