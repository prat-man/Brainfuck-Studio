package in.pratanumandal.brainfuck.engine.processor.interpreter;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.engine.processor.Processor;
import in.pratanumandal.brainfuck.gui.TabData;

public abstract class Interpreter extends Processor {

    protected Interpreter(TabData tabData) {
        super(tabData);
    }

    public static Interpreter getInterpreter(TabData tabData) {
        Integer cellSize = Configuration.getCellSize();

        if (cellSize == 8) {
            return new Interpreter8(tabData);
        }
        else if (cellSize == 16) {
            return new Interpreter16(tabData);
        }

        return null;
    }

}
