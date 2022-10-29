package in.pratanumandal.brainfuck.engine.processor.translator;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.IOException;

public class JavaTranslatorFast extends Translator {

    public JavaTranslatorFast(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, BufferedWriter bw) throws IOException {
        if (this.cellSize == 8) {
            bw.write("import java.io.*;\n" +
                    "\n" +
                    "public class " + this.getFileNameWithoutExtension() + " {\n" +
                    "\n" +
                    "\tpublic static final int MEMORY_SIZE = " + Configuration.getMemorySize() + ";\n" +
                    "\n" +
                    "\tpublic static final Integer NO_JUMP = 0;\n" +
                    "\tpublic static final Character SET_ZERO = '!';\n" +
                    "\tpublic static final Character SCAN_ZERO_LEFT = '@';\n" +
                    "\tpublic static final Character SCAN_ZERO_RIGHT = '#';\n" +
                    "\tpublic static final Character ADDRESS = '$';\n" +
                    "\tpublic static final Character DATA = '%';\n" +
                    "\n" +
                    "\tpublic static final Reader CR = System.console().reader();\n" +
                    "\n" +
                    "\tpublic static final char[] processed = {" + Utils.join(processed) + "};\n" +
                    "\tpublic static final int[] jumps = {" + Utils.join(jumps) + "};\n" +
                    "\tpublic static final byte[] memory = new byte[MEMORY_SIZE];\n" +
                    "\n" +
                    "\tpublic static int pointer = 0;\n" +
                    "\n" +
                    "\tpublic static int findZeroLeft(int position) {\n" +
                    "\t\tfor (int i = position; i >= 0; i--) {\n" +
                    "\t\t\tif (memory[i] == 0) {\n" +
                    "\t\t\t\treturn i;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\tfor (int i = MEMORY_SIZE - 1; i > position; i--) {\n" +
                    "\t\t\tif (memory[i] == 0) {\n" +
                    "\t\t\t\treturn i;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\treturn -1;\n" +
                    "\t}\n" +
                    "\n" +
                    "\tpublic static int findZeroRight(int position) {\n" +
                    "\t\tfor (int i = position; i < MEMORY_SIZE; i++) {\n" +
                    "\t\t\tif (memory[i] == 0) {\n" +
                    "\t\t\t\treturn i;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\tfor (int i = 0; i < position; i++) {\n" +
                    "\t\t\tif (memory[i] == 0) {\n" +
                    "\t\t\t\treturn i;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\treturn -1;\n" +
                    "\t}\n" +
                    "\n" +
                    "\tpublic static void main(String[] args) throws IOException {\n" +
                    "\n" +
                    "\t\tfor (int i = 0; i < processed.length; i++) {\n" +
                    "\t\t\tchar ch = processed[i];\n" +
                    "\n" +
                    "\t\t\t// handle pointer movement (> and <)\n" +
                    "\t\t\tif (ch == ADDRESS) {\n" +
                    "\t\t\t\tint sum = jumps[i];\n" +
                    "\t\t\t\tpointer += sum;\n" +
                    "\t\t\t\tif (pointer < 0 || pointer >= memory.length) {\n" +
                    "\t\t\t\t\tSystem.out.printf(\"\\nError: Memory index out of bounds %d\\n\", pointer);\n" +
                    "\t\t\t\t\tSystem.exit(1);\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle value update (+ and -)\n" +
                    "\t\t\telse if (ch == DATA) {\n" +
                    "\t\t\t\tint sum = jumps[i];\n" +
                    "\t\t\t\tmemory[pointer] = (byte) (memory[pointer] + sum);\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle output (.)\n" +
                    "\t\t\telse if (ch == '.') {\n" +
                    "\t\t\t\tSystem.out.printf(\"%c\", memory[pointer] >= 0 ? memory[pointer] : memory[pointer] + 256);\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle input (,)\n" +
                    "\t\t\telse if (ch == ',') {\n" +
                    "\t\t\t\tmemory[pointer] = (byte) CR.read();\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle [-]\n" +
                    "\t\t\telse if (ch == SET_ZERO) {\n" +
                    "\t\t\t\tmemory[pointer] = 0;\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle [<]\n" +
                    "\t\t\telse if (ch == SCAN_ZERO_LEFT) {\n" +
                    "\t\t\t\tpointer = findZeroLeft(pointer);\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle [>]\n" +
                    "\t\t\telse if (ch == SCAN_ZERO_RIGHT) {\n" +
                    "\t\t\t\tpointer = findZeroRight(pointer);\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle loop opening ([)\n" +
                    "\t\t\telse if (ch == '[') {\n" +
                    "\t\t\t\tif (memory[pointer] == 0) {\n" +
                    "\t\t\t\t\ti = jumps[i];\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle loop closing (])\n" +
                    "\t\t\telse if (ch == ']') {\n" +
                    "\t\t\t\tif (memory[pointer] != 0) {\n" +
                    "\t\t\t\t\ti = jumps[i];\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\n" +
                    "\t}\n" +
                    "\n" +
                    "}\n");
        }
        else if (this.cellSize == 16) {
            bw.write("import java.io.*;\n" +
                    "\n" +
                    "public class " + this.getFileNameWithoutExtension() + " {\n" +
                    "\n" +
                    "\tpublic static final int MEMORY_SIZE = " + Configuration.getMemorySize() + ";\n" +
                    "\n" +
                    "\tpublic static final Integer NO_JUMP = 0;\n" +
                    "\tpublic static final Character SET_ZERO = '!';\n" +
                    "\tpublic static final Character SCAN_ZERO_LEFT = '@';\n" +
                    "\tpublic static final Character SCAN_ZERO_RIGHT = '#';\n" +
                    "\tpublic static final Character ADDRESS = '$';\n" +
                    "\tpublic static final Character DATA = '%';\n" +
                    "\n" +
                    "\tpublic static final Reader CR = System.console().reader();\n" +
                    "\n" +
                    "\tpublic static final char[] processed = {" + Utils.join(processed) + "};\n" +
                    "\tpublic static final int[] jumps = {" + Utils.join(jumps) + "};\n" +
                    "\tpublic static final short[] memory = new short[MEMORY_SIZE];\n" +
                    "\n" +
                    "\tpublic static int pointer = 0;\n" +
                    "\n" +
                    "\tpublic static int findZeroLeft(int position) {\n" +
                    "\t\tfor (int i = position; i >= 0; i--) {\n" +
                    "\t\t\tif (memory[i] == 0) {\n" +
                    "\t\t\t\treturn i;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\tfor (int i = MEMORY_SIZE - 1; i > position; i--) {\n" +
                    "\t\t\tif (memory[i] == 0) {\n" +
                    "\t\t\t\treturn i;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\treturn -1;\n" +
                    "\t}\n" +
                    "\n" +
                    "\tpublic static int findZeroRight(int position) {\n" +
                    "\t\tfor (int i = position; i < MEMORY_SIZE; i++) {\n" +
                    "\t\t\tif (memory[i] == 0) {\n" +
                    "\t\t\t\treturn i;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\tfor (int i = 0; i < position; i++) {\n" +
                    "\t\t\tif (memory[i] == 0) {\n" +
                    "\t\t\t\treturn i;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\treturn -1;\n" +
                    "\t}\n" +
                    "\n" +
                    "\tpublic static void main(String[] args) throws IOException {\n" +
                    "\n" +
                    "\t\tfor (int i = 0; i < processed.length; i++) {\n" +
                    "\t\t\tchar ch = processed[i];\n" +
                    "\n" +
                    "\t\t\t// handle pointer movement (> and <)\n" +
                    "\t\t\tif (ch == ADDRESS) {\n" +
                    "\t\t\t\tint sum = jumps[i];\n" +
                    "\t\t\t\tpointer += sum;\n" +
                    "\t\t\t\tif (pointer < 0 || pointer >= memory.length) {\n" +
                    "\t\t\t\t\tSystem.out.printf(\"\\nError: Memory index out of bounds %d\\n\", pointer);\n" +
                    "\t\t\t\t\tSystem.exit(1);\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle value update (+ and -)\n" +
                    "\t\t\telse if (ch == DATA) {\n" +
                    "\t\t\t\tint sum = jumps[i];\n" +
                    "\t\t\t\tmemory[pointer] = (short) (memory[pointer] + sum);\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle output (.)\n" +
                    "\t\t\telse if (ch == '.') {\n" +
                    "\t\t\t\tSystem.out.printf(\"%c\", memory[pointer] >= 0 ? memory[pointer] : memory[pointer] + 256);\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle input (,)\n" +
                    "\t\t\telse if (ch == ',') {\n" +
                    "\t\t\t\tmemory[pointer] = (short) CR.read();\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle [-]\n" +
                    "\t\t\telse if (ch == SET_ZERO) {\n" +
                    "\t\t\t\tmemory[pointer] = 0;\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle [<]\n" +
                    "\t\t\telse if (ch == SCAN_ZERO_LEFT) {\n" +
                    "\t\t\t\tpointer = findZeroLeft(pointer);\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle [>]\n" +
                    "\t\t\telse if (ch == SCAN_ZERO_RIGHT) {\n" +
                    "\t\t\t\tpointer = findZeroRight(pointer);\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle loop opening ([)\n" +
                    "\t\t\telse if (ch == '[') {\n" +
                    "\t\t\t\tif (memory[pointer] == 0) {\n" +
                    "\t\t\t\t\ti = jumps[i];\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t\t// handle loop closing (])\n" +
                    "\t\t\telse if (ch == ']') {\n" +
                    "\t\t\t\tif (memory[pointer] != 0) {\n" +
                    "\t\t\t\t\ti = jumps[i];\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\n" +
                    "\t}\n" +
                    "\n" +
                    "}\n");
        }
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
