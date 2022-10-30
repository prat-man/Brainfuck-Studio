package in.pratanumandal.brainfuck.engine.processor.translator;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.IOException;

public class JavaTranslator extends Translator {

    public JavaTranslator(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, BufferedWriter bw) throws IOException {
        bw.write("import java.io.*;\n\n");
        bw.write("public class " + this.getFileNameWithoutExtension() + " {\n\n");
        bw.write("\tpublic static final int MEMORY_SIZE = " + Configuration.getMemorySize() + ";\n\n");
        bw.write("\tpublic static final Reader CR = System.console().reader();\n\n");

        if (this.cellSize == 8) {
            bw.write("\tpublic static final byte[] memory = new byte[MEMORY_SIZE];\n\n");
        }
        else if (this.cellSize == 16) {
            bw.write("\tpublic static final short[] memory = new short[MEMORY_SIZE];\n\n");
        }

        bw.write("\tpublic static int pointer = 0;\n\n");
        bw.write("\tpublic static int findZeroLeft(int position) {\n\t\tfor (int i = position; i >= 0; i--) {\n\t\t\tif (memory[i] == 0) {\n\t\t\t\treturn i;\n\t\t\t}\n\t\t}\n\t\tfor (int i = MEMORY_SIZE - 1; i > position; i--) {\n\t\t\tif (memory[i] == 0) {\n\t\t\t\treturn i;\n\t\t\t}\n\t\t}\n\t\treturn -1;\n\t}\n\n");
        bw.write("\tpublic static int findZeroRight(int position) {\n\t\tfor (int i = position; i < MEMORY_SIZE; i++) {\n\t\t\tif (memory[i] == 0) {\n\t\t\t\treturn i;\n\t\t\t}\n\t\t}\n\t\tfor (int i = 0; i < position; i++) {\n\t\t\tif (memory[i] == 0) {\n\t\t\t\treturn i;\n\t\t\t}\n\t\t}\n\t\treturn -1;\n\t}\n\n");
        bw.write("\tpublic static int getUpdatedPointer(int pointer, int sum) {\n\t\tpointer += sum;\n\t\tif (pointer < 0 || pointer >= MEMORY_SIZE) {\n\t\t\tSystem.out.printf(\"\\nError: Memory index out of bounds %d\\n\", pointer);\n\t\t\tSystem.exit(1);\n\t\t}\n\t\treturn pointer;\n\t}\n\n");
        bw.write("\tpublic static void main(String[] args) throws IOException {\n\n");

        String indent = "\t\t";

        for (int i = 0; i < processed.length && !this.kill.get(); i++) {
            if (i % 50 == 0) {
                double progress = i / (double) processed.length;
                Utils.runAndWait(() -> notification.setProgress(progress));
            }

            char ch = processed[i];

            // handle pointer movement (> and <)
            if (ch == ADDRESS) {
                int sum = jumps[i];
                bw.write(indent + "pointer = getUpdatedPointer(pointer, " + sum + ");\n");
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];
                bw.write(indent + "memory[pointer] += " + sum + ";\n");
            }
            // handle output (.)
            else if (ch == '.') {
                bw.write(indent + "System.out.printf(\"%c\", memory[pointer] >= 0 ? memory[pointer] : memory[pointer] + 256);\n");
            }
            // handle input (,)
            else if (ch == ',') {
                if (this.cellSize == 8) {
                    bw.write(indent + "memory[pointer] = (byte) CR.read();\n");
                }
                else if (this.cellSize == 16) {
                    bw.write(indent + "memory[pointer] = (short) CR.read();\n");
                }
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

    @Override
    public FileChooser.ExtensionFilter getExtensionFilter() {
        return new FileChooser.ExtensionFilter("Java files (*.java)", "*.java");
    }

}
