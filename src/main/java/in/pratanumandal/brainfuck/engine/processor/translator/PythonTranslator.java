package in.pratanumandal.brainfuck.engine.processor.translator;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import javafx.stage.FileChooser;

import java.io.IOException;

public class PythonTranslator extends Translator {

    public PythonTranslator(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, TranslationWriter writer) throws IOException {
        String datatype = (this.cellSize == 8) ? "ubyte" : (this.cellSize == 16) ? "ushort" : "uintc";

        writer.writeLine("import sys");
        writer.writeLine("import numpy as np");
        writer.writeLine();
        writer.writeLine("np.seterr(over='ignore')");
        writer.writeLine();
        writer.writeLine("MEMORY_SIZE = " + Configuration.getMemorySize());
        writer.writeLine();

        writer.writeLine();
        writer.writeLine("def findZeroLeft(memory, position):");
        writer.updateIndentation(1);
        writer.writeLine("for i in range(position, 0, -1):");
        writer.updateIndentation(1);
        writer.writeLine("if memory[i] == 0:");
        writer.updateIndentation(1);
        writer.writeLine("return i");
        writer.updateIndentation(-2);
        writer.writeLine("for i in range(MEMORY_SIZE - 1, position, -1):");
        writer.updateIndentation(1);
        writer.writeLine("if memory[i] == 0:");
        writer.updateIndentation(1);
        writer.writeLine("return i");
        writer.updateIndentation(-2);
        writer.writeLine("return -1");
        writer.updateIndentation(-1);
        writer.writeLine();

        writer.writeLine();
        writer.writeLine("def findZeroRight(memory, position):");
        writer.updateIndentation(1);
        writer.writeLine("for i in range(position, MEMORY_SIZE, 1):");
        writer.updateIndentation(1);
        writer.writeLine("if memory[i] == 0:");
        writer.updateIndentation(1);
        writer.writeLine("return i");
        writer.updateIndentation(-2);
        writer.writeLine("for i in range(0, position, 1):");
        writer.updateIndentation(1);
        writer.writeLine("if memory[i] == 0:");
        writer.updateIndentation(1);
        writer.writeLine("return i");
        writer.updateIndentation(-2);
        writer.writeLine("return -1");
        writer.updateIndentation(-1);
        writer.writeLine();

        writer.writeLine();
        writer.writeLine("def updatePointer(pointer, sum):");
        writer.updateIndentation(1);
        writer.writeLine("pointer = pointer + sum");
        if (this.wrapMemory) {
            writer.writeLine("if pointer < 0:");
            writer.updateIndentation(1);
            writer.writeLine("pointer += MEMORY_SIZE");
            writer.updateIndentation(-1);

            writer.writeLine("elif pointer >= MEMORY_SIZE:");
            writer.updateIndentation(1);
            writer.writeLine("pointer -= MEMORY_SIZE");
            writer.updateIndentation(-1);
        }
        else {
            writer.writeLine("if pointer < 0 or pointer >= MEMORY_SIZE:");
            writer.updateIndentation(1);
            writer.writeLine("print(\"\\nError: Memory index out of bounds \" + str(pointer))");
            writer.writeLine("exit(1)");
            writer.updateIndentation(-1);
        }
        writer.writeLine("return pointer");
        writer.updateIndentation(-1);
        writer.writeLine();

        writer.writeLine();
        writer.writeLine("def main():");
        writer.updateIndentation(1);
        writer.writeLine("memory = np.zeros((MEMORY_SIZE), dtype=np." + datatype + ")");
        writer.writeLine("pointer = 0");
        writer.writeLine();

        for (int i = 0; i < processed.length && !this.kill.get(); i++) {
            if (i % 50 == 0) {
                double progress = i / (double) processed.length;
                Utils.runAndWait(() -> notification.setProgress(progress));
            }

            char ch = processed[i];

            // handle pointer movement (> and <)
            if (ch == ADDRESS) {
                int sum = jumps[i];
                writer.writeLine("pointer = updatePointer(pointer, " + sum + ")");
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];
                writer.writeLine("memory[pointer] += np." + datatype + "(" + sum + ")");
            }
            // handle output (.)
            else if (ch == '.') {
                writer.writeLine("print(chr(memory[pointer]), end='', flush=True)");
            }
            // handle input (,)
            else if (ch == ',') {
                writer.writeLine("memory[pointer] = np." + datatype + "(ord(sys.stdin.read(1)))");
            }
            // handle [-]
            else if (ch == SET_ZERO) {
                writer.writeLine("memory[pointer] = 0");
            }
            // handle [<]
            else if (ch == SCAN_ZERO_LEFT) {
                writer.writeLine("pointer = findZeroLeft(memory, pointer)");
            }
            // handle [>]
            else if (ch == SCAN_ZERO_RIGHT) {
                writer.writeLine("pointer = findZeroRight(memory, pointer)");
            }
            // handle loop opening ([)
            else if (ch == '[') {
                writer.writeLine("while memory[pointer] != 0:");
                writer.updateIndentation(1);
            }
            // handle loop closing (])
            else if (ch == ']') {
                writer.updateIndentation(-1);
            }
        }

        writer.updateIndentation(-1);
        writer.writeLine();

        writer.writeLine();
        writer.writeLine("if __name__ == \"__main__\":");
        writer.updateIndentation(1);
        writer.writeLine("main()");
    }

    @Override
    public String getLanguage() {
        return "Python";
    }

    @Override
    public String getExtension() {
        return "py";
    }

    @Override
    public FileChooser.ExtensionFilter getExtensionFilter() {
        return new FileChooser.ExtensionFilter("Python files (*.py)", "*.py");
    }

}
