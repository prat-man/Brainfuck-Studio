package in.pratanumandal.brainfuck.engine.processor.translator;

import com.sun.jna.Platform;
import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import javafx.stage.FileChooser;

import java.io.IOException;

public class JavaTranslator extends Translator {

    public JavaTranslator(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, TranslationWriter writer) throws IOException {
        String datatype = (this.cellSize == 8) ? "byte" : (this.cellSize == 16) ? "short" : "int";
        String wrapperDatatype = (this.cellSize == 8) ? "Byte" : (this.cellSize == 16) ? "Short" : "Int";

        writer.writeLine("import java.io.*;");
        writer.writeLine();
        writer.writeLine("public class " + this.getFileNameWithoutExtension() + " {");
        writer.writeLine();
        writer.updateIndentation(1);

        writer.writeLine("public static final int MEMORY_SIZE = " + Configuration.getMemorySize() + ";");
        writer.writeLine();

        writer.writeLine("public static int findZeroLeft(" + datatype + "[] memory, int position) {");
        writer.updateIndentation(1);
        writer.writeLine("for (int i = position; i >= 0; i--) {");
        writer.updateIndentation(1);
        writer.writeLine("if (memory[i] == 0) return i;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine("for (int i = MEMORY_SIZE - 1; i > position; i--) {");
        writer.updateIndentation(1);
        writer.writeLine("if (memory[i] == 0) return i;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine("return -1;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine();

        writer.writeLine("public static int findZeroRight(" + datatype + "[] memory, int position) {");
        writer.updateIndentation(1);
        writer.writeLine("for (int i = position; i < MEMORY_SIZE; i++) {");
        writer.updateIndentation(1);
        writer.writeLine("if (memory[i] == 0) return i;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine("for (int i = 0; i < position; i++) {");
        writer.updateIndentation(1);
        writer.writeLine("if (memory[i] == 0) return i;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine("return -1;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine();

        writer.writeLine("public static int updatePointer(int pointer, int sum) {");
        writer.updateIndentation(1);
        writer.writeLine("pointer += sum;");
        if (this.wrapMemory) {
            writer.writeLine("if (pointer < 0) pointer += MEMORY_SIZE;");
            writer.writeLine("else if (pointer >= MEMORY_SIZE) pointer -= MEMORY_SIZE;");
        }
        else {
            writer.writeLine("if (pointer < 0 || pointer >= MEMORY_SIZE) {");
            writer.updateIndentation(1);
            writer.writeLine("System.out.printf(\"\\nError: Memory index out of bounds %d\\n\", pointer);");
            writer.writeLine("System.exit(1);");
            writer.updateIndentation(-1);
            writer.writeLine("}");
        }
        writer.writeLine("return pointer;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine();

        writer.writeLine("public static String getSymbol(" + wrapperDatatype + " value) {");
        writer.updateIndentation(1);
        writer.writeLine("Long codePoint = " + wrapperDatatype + ".toUnsignedLong(value);");
        writer.writeLine("return String.valueOf(Character.toChars(codePoint.intValue()));");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine();

        writer.writeLine("public static void main(String[] args) throws IOException {");
        writer.updateIndentation(1);

        if (Platform.getOSType() == Platform.WINDOWS) {
            writer.writeLine("ProcessBuilder pb = new ProcessBuilder(\"cmd.exe\", \"/c\", \"chcp\", \"65001\", \">\", \"nul\").inheritIO();");
            writer.writeLine("Process p = pb.start();");
            writer.writeLine("try {");
            writer.updateIndentation(1);
            writer.writeLine("p.waitFor();");
            writer.updateIndentation(-1);
            writer.writeLine("} catch (InterruptedException e) {}");
            writer.writeLine();
            writer.writeLine("PrintStream out = new PrintStream(System.out, true, \"UTF-8\");");
            writer.writeLine();
        }
        else {
            writer.writeLine("PrintStream out = System.out;");
            writer.writeLine();
        }

        writer.writeLine(datatype + "[] memory = new " + datatype + "[MEMORY_SIZE];");
        writer.writeLine("int pointer = 0;");
        writer.writeLine();
        writer.writeLine("try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {");
        writer.updateIndentation(1);

        for (int i = 0; i < processed.length && !this.kill.get(); i++) {
            if (i % 50 == 0) {
                double progress = i / (double) processed.length;
                Utils.runAndWait(() -> notification.setProgress(progress));
            }

            char ch = processed[i];

            // handle pointer movement (> and <)
            if (ch == ADDRESS) {
                int sum = jumps[i];
                writer.writeLine("pointer = updatePointer(pointer, " + sum + ");");
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];
                writer.writeLine("memory[pointer] += " + sum + ";");
            }
            // handle output (.)
            else if (ch == '.') {
                writer.writeLine("out.print(getSymbol(memory[pointer]));");
            }
            // handle input (,)
            else if (ch == ',') {
                writer.writeLine("memory[pointer] = (" + datatype + ") br.read();");
            }
            // handle [-]
            else if (ch == SET_ZERO) {
                writer.writeLine("memory[pointer] = 0;");
            }
            // handle [<]
            else if (ch == SCAN_ZERO_LEFT) {
                writer.writeLine("pointer = findZeroLeft(memory, pointer);");
            }
            // handle [>]
            else if (ch == SCAN_ZERO_RIGHT) {
                writer.writeLine("pointer = findZeroRight(memory, pointer);");
            }
            // handle loop opening ([)
            else if (ch == '[') {
                writer.writeLine("while (memory[pointer] != 0) {");
                writer.updateIndentation(1);
            }
            // handle loop closing (])
            else if (ch == ']') {
                writer.updateIndentation(-1);
                writer.writeLine("}");
            }
        }

        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine();

        writer.updateIndentation(-1);
        writer.writeLine("}");
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
