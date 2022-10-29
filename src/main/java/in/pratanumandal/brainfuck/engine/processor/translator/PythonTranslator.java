package in.pratanumandal.brainfuck.engine.processor.translator;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.IOException;

public class PythonTranslator extends Translator {

    public PythonTranslator(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, BufferedWriter bw) throws IOException {
        bw.write("import sys\n");
        bw.write("import numpy as np\n\n");
        bw.write("np.seterr(over='ignore')\n\n");
        bw.write("MEMORY_SIZE = " + Configuration.getMemorySize() + "\n\n");

        if (this.cellSize == 8) {
            bw.write("memory = np.zeros((MEMORY_SIZE), dtype=np.ubyte)\n");
        }
        else if (this.cellSize == 16) {
            bw.write("memory = np.zeros((MEMORY_SIZE), dtype=np.uintc)\n");
        }

        bw.write("pointer = 0\n\n");
        bw.write("def findZeroLeft(position):\n\tfor i in range(position, 0, -1):\n\t\tif memory[i] == 0:\n\t\t\treturn i\n\tfor i in range(MEMORY_SIZE - 1, position, -1):\n\t\tif memory[i] == 0:\n\t\t\treturn i\n\treturn -1\n\n");
        bw.write("def findZeroRight(position):\n\tfor i in range(position, MEMORY_SIZE, 1):\n\t\tif memory[i] == 0:\n\t\t\treturn i\n\tfor i in range(0, position, 1):\n\t\tif memory[i] == 0:\n\t\t\treturn i\n\treturn -1\n\n");

        String indent = "";

        for (int i = 0; i < processed.length && !this.kill.get(); i++) {
            if (i % 50 == 0) {
                double progress = i / (double) processed.length;
                Utils.runAndWait(() -> notification.setProgress(progress));
            }

            char ch = processed[i];

            // handle pointer movement (> and <)
            if (ch == ADDRESS) {
                int sum = jumps[i];

                bw.write(indent + "pointer = pointer + " + sum + "\n");
                bw.write(indent + "if pointer < 0 or pointer >= MEMORY_SIZE:\n");
                bw.write(indent + "\tprint(\"\\nError: Memory index out of bounds \" + str(pointer))\n");
                bw.write(indent + "\texit(1)\n");
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];

                if (this.cellSize == 8) {
                    bw.write(indent + "memory[pointer] = memory[pointer] + np.ubyte(" + sum + ")\n");
                }
                else if (this.cellSize == 16) {
                    bw.write(indent + "memory[pointer] = memory[pointer] + np.uintc(" + sum + ")\n");
                }
            }
            // handle output (.)
            else if (ch == '.') {
                bw.write(indent + "print(chr(memory[pointer]), end='', flush=True)\n");
            }
            // handle input (,)
            else if (ch == ',') {
                if (this.cellSize == 8) {
                    bw.write(indent + "memory[pointer] = np.ubyte(ord(sys.stdin.read(1)))\n");
                }
                else if (this.cellSize == 16) {
                    bw.write(indent + "memory[pointer] = np.uintc(ord(sys.stdin.read(1)))\n");
                }
            }
            // handle [-]
            else if (ch == SET_ZERO) {
                bw.write(indent + "memory[pointer] = 0\n");
            }
            // handle [<]
            else if (ch == SCAN_ZERO_LEFT) {
                bw.write(indent + "pointer = findZeroLeft(pointer)\n");
            }
            // handle [>]
            else if (ch == SCAN_ZERO_RIGHT) {
                bw.write(indent + "pointer = findZeroRight(pointer)\n");
            }
            // handle loop opening ([)
            else if (ch == '[') {
                bw.write(indent + "while memory[pointer] != 0:\n");
                indent += '\t';
            }
            // handle loop closing (])
            else if (ch == ']') {
                indent = indent.substring(0, indent.length() - 1);
            }
        }
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
