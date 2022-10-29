package in.pratanumandal.brainfuck.engine.processor.translator;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.IOException;

public class CTranslator extends Translator {

    public CTranslator(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, BufferedWriter bw) throws IOException {
        bw.write("#include<stdio.h>\n");
        bw.write("#include<wchar.h>\n");
        bw.write("#include<string.h>\n");
        bw.write("#include<locale.h>\n\n");
        bw.write("#ifdef _WIN32\n#include<stdlib.h>\n#endif\n\n");
        bw.write("#define MEMORY_SIZE " + Configuration.getMemorySize() + "\n\n");

        if (this.cellSize == 8) {
            bw.write("unsigned char memory[MEMORY_SIZE];\n");
        }
        else if (this.cellSize == 16) {
            bw.write("wchar_t memory[MEMORY_SIZE];\n");
        }

        bw.write("int pointer = 0;\n\n");
        bw.write("static inline int findZeroLeft(int position) {\n\tfor (int i = position; i >= 0; i--) {\n\t\tif (memory[i] == 0) {\n\t\t\treturn i;\n\t\t}\n\t}\n\tfor (int i = MEMORY_SIZE - 1; i > position; i--) {\n\t\tif (memory[i] == 0) {\n\t\t\treturn i;\n\t\t}\n\t}\n\treturn -1;\n}\n\n");
        bw.write("static inline int findZeroRight(int position) {\n\tfor (int i = position; i < MEMORY_SIZE; i++) {\n\t\tif (memory[i] == 0) {\n\t\t\treturn i;\n\t\t}\n\t}\n\tfor (int i = 0; i < position; i++) {\n\t\tif (memory[i] == 0) {\n\t\t\treturn i;\n\t\t}\n\t}\n\treturn -1;\n}\n\n");
        bw.write("int main() {\n");
        bw.write("#ifdef _WIN32\n\tsystem(\"chcp 1252 > nul\");\n#endif\n");
        bw.write("\tsetlocale(LC_ALL, \"\");\n\n");
        bw.write("\tmemset(memory, 0, MEMORY_SIZE);\n\n");
        bw.write("\twchar_t ch;\n\n");

        String indent = "\t";

        for (int i = 0; i < processed.length && !this.kill.get(); i++) {
            if (i % 50 == 0) {
                double progress = i / (double) processed.length;
                Utils.runAndWait(() -> notification.setProgress(progress));
            }

            char ch = processed[i];

            // handle pointer movement (> and <)
            if (ch == ADDRESS) {
                int sum = jumps[i];

                bw.write(indent + "pointer += " + sum + ";\n");
                bw.write(indent + "if (pointer < 0 || pointer >= MEMORY_SIZE) {\n");
                bw.write(indent + "\tprintf(\"\\nError: Memory index out of bounds %d\\n\", pointer);\n");
                bw.write(indent + "\treturn 1;\n");
                bw.write(indent + "}\n");
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];
                bw.write(indent + "memory[pointer] += " + sum + ";\n");
            }
            // handle output (.)
            else if (ch == '.') {
                bw.write(indent + "printf(\"%lc\", memory[pointer]);\n");
                bw.write(indent + "fflush(stdout);\n");
            }
            // handle input (,)
            else if (ch == ',') {
                bw.write(indent + "scanf(\"%lc\", &ch);\n");
                bw.write(indent + "memory[pointer] = ch;\n");
            }
            // handle [-]
            else if (ch == SET_ZERO) {
                bw.write(indent + "memory[pointer] = 0;\n");
            }
            // handle [<]
            else if (ch == SCAN_ZERO_LEFT) {
                bw.write(indent + "pointer = findZeroLeft(pointer);\n");
            }
            // handle [>]
            else if (ch == SCAN_ZERO_RIGHT) {
                bw.write(indent + "pointer = findZeroRight(pointer);\n");
            }
            // handle loop opening ([)
            else if (ch == '[') {
                bw.write(indent + "while (memory[pointer] != 0) {\n");
                indent += '\t';
            }
            // handle loop closing (])
            else if (ch == ']') {
                indent = indent.substring(0, indent.length() - 1);
                bw.write(indent + "}\n");
            }
        }

        bw.write("\n\treturn 0;\n}\n");
    }

    @Override
    public String getLanguage() {
        return "C";
    }

    @Override
    public String getExtension() {
        return "c";
    }

    @Override
    public FileChooser.ExtensionFilter getExtensionFilter() {
        return new FileChooser.ExtensionFilter("C files (*.c)", "*.c");
    }

}
