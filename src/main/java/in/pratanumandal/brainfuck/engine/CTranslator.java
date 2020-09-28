package in.pratanumandal.brainfuck.engine;

import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.application.Platform;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class CTranslator extends Translator {

    public CTranslator(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, BufferedWriter bw) throws IOException {
        bw.write("#include<stdio.h>\n");
        bw.write("#include<string.h>\n\n");
        bw.write("#define MEMORY_SIZE " + Constants.MEMORY_SIZE + "\n\n");
        bw.write("unsigned char memory[MEMORY_SIZE];\n");
        bw.write("int pointer = 0;\n\n");
        bw.write("int findZeroLeft(int position) {\n\tfor (int i = position; i >= 0; i--) {\n\t\tif (memory[i] == 0) {\n\t\t\treturn i;\n\t\t}\n\t}\n\tfor (int i = MEMORY_SIZE - 1; i > position; i--) {\n\t\tif (memory[i] == 0) {\n\t\t\treturn i;\n\t\t}\n\t}\n\treturn -1;\n}\n\n");
        bw.write("int findZeroRight(int position) {\n\tfor (int i = position; i < MEMORY_SIZE; i++) {\n\t\tif (memory[i] == 0) {\n\t\t\treturn i;\n\t\t}\n\t}\n\tfor (int i = 0; i < position; i++) {\n\t\tif (memory[i] == 0) {\n\t\t\treturn i;\n\t\t}\n\t}\n\treturn -1;\n}\n\n");
        bw.write("int main() {\n");
        bw.write("\tmemset(memory, 0, MEMORY_SIZE);\n\n");

        String indent = "\t";
        int length = new String(processed).trim().length();

        for (int i = 0; i < processed.length && !this.kill.get(); i++) {
            if (i % 50 == 0) {
                double progress = i / (double) length;
                Utils.runAndWait(() -> notification.setProgress(progress));
            }

            char ch = processed[i];

            if (ch == '\0') break;

            // handle pointer movement (> and <)
            if (ch == ADDRESS) {
                int sum = jumps[i];

                bw.write(indent + "pointer += " + sum + ";\n");
                bw.write(indent + "if (pointer >= MEMORY_SIZE) pointer -= MEMORY_SIZE;\n");
                bw.write(indent + "else if (pointer < 0) pointer += MEMORY_SIZE;\n");
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];
                bw.write(indent + "memory[pointer] += " + sum + ";\n");
            }
            // handle output (.)
            else if (ch == '.') {
                bw.write(indent + "printf(\"%%c\", memory[pointer]);\n");
                bw.write(indent + "fflush(stdout);\n");
            }
            // handle input (,)
            else if (ch == ',') {
                bw.write(indent + "memory[pointer] = getchar();\n");
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

}
