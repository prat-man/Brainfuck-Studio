package in.pratanumandal.brainfuck.engine.processor.translator;

import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;

import java.io.BufferedWriter;
import java.io.IOException;

public class JavaTranslator extends Translator {

    public JavaTranslator(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, BufferedWriter bw) throws IOException {
        bw.write("import java.util.Scanner;\n\n");
        bw.write("public class " + this.getFileNameWithoutExtension() + " {\n\n");
        bw.write("\tpublic static final int MEMORY_SIZE = " + Constants.MEMORY_SIZE + ";\n\n");
        bw.write("\tpublic static final Scanner SC = new Scanner(System.in);\n\n");
        bw.write("\tpublic static final char[] memory = new char[MEMORY_SIZE];\n\n");
        bw.write("\tpublic static int findZeroLeft(int position) {\n\t\tfor (int i = position; i >= 0; i--) {\n\t\t\tif (memory[i] == 0) {\n\t\t\t\treturn i;\n\t\t\t}\n\t\t}\n\t\tfor (int i = MEMORY_SIZE - 1; i > position; i--) {\n\t\t\tif (memory[i] == 0) {\n\t\t\t\treturn i;\n\t\t\t}\n\t\t}\n\t\treturn -1;\n\t}\n\n");
        bw.write("\tpublic static int findZeroRight(int position) {\n\t\tfor (int i = position; i < MEMORY_SIZE; i++) {\n\t\t\tif (memory[i] == 0) {\n\t\t\t\treturn i;\n\t\t\t}\n\t\t}\n\t\tfor (int i = 0; i < position; i++) {\n\t\t\tif (memory[i] == 0) {\n\t\t\t\treturn i;\n\t\t\t}\n\t\t}\n\t\treturn -1;\n\t}\n\n");
        bw.write("\tpublic static void main(String[] args) {\n\n");
        bw.write("\t\tint pointer = 0;\n\n");

        String indent = "\t\t";
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
                bw.write(indent + "System.out.printf(\"%c\", memory[pointer]);\n");
            }
            // handle input (,)
            else if (ch == ',') {
                bw.write(indent + "memory[pointer] = SC.next(\".\").charAt(0);\n");
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

        bw.write("\n\t}\n\n}\n");
    }

    @Override
    public String getLanguage() {
        return "Java";
    }

    @Override
    public String getExtension() {
        return "java";
    }

}
