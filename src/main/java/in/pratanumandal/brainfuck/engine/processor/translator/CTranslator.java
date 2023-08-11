package in.pratanumandal.brainfuck.engine.processor.translator;

import com.sun.jna.Platform;
import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import javafx.stage.FileChooser;

import java.io.IOException;

public class CTranslator extends Translator {

    public CTranslator(TabData tabData) {
        super(tabData);
    }

    @Override
    public void doTranslate(NotificationManager.Notification notification, TranslationWriter writer) throws IOException {
        String datatype = (this.cellSize == 8) ? "unsigned char" : (this.cellSize == 16) ? "unsigned short" : "unsigned int";

        writer.writeLine("#include <stdio.h>");
        writer.writeLine("#include <stdlib.h>");
        writer.writeLine("#include <string.h>");
        writer.writeLine();
        writer.writeLine("#define MEMORY_SIZE " + Configuration.getMemorySize());
        writer.writeLine();

        writer.writeLine("int findZeroLeft(" + datatype + " *memory, int position) {");
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

        writer.writeLine("int findZeroRight(" + datatype + " *memory, int position) {");
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

        writer.writeLine("int updatePointer(int pointer, int sum) {");
        writer.updateIndentation(1);
        writer.writeLine("pointer += sum;");
        if (this.wrapMemory) {
            writer.writeLine("if (pointer < 0) pointer += MEMORY_SIZE;");
            writer.writeLine("else if (pointer >= MEMORY_SIZE) pointer -= MEMORY_SIZE;");
        }
        else {
            writer.writeLine("if (pointer < 0 || pointer >= MEMORY_SIZE) {");
            writer.updateIndentation(1);
            writer.writeLine("printf(\"\\nError: Memory index out of bounds %d\\n\", pointer);");
            writer.writeLine("exit(1);");
            writer.updateIndentation(-1);
            writer.writeLine("}");
        }
        writer.writeLine("return pointer;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine();

        writer.writeLine("char *getSymbol(" + datatype + " codepoint) {");
        writer.updateIndentation(1);
        writer.writeLine("char *out = calloc(5, sizeof(char));");
        writer.writeLine();
        writer.writeLine("if (codepoint <= 0x7f) {");
        writer.updateIndentation(1);
        writer.writeLine("out[0] = (char) codepoint;");
        writer.writeLine("out[1] = '\\0';");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine("else if (codepoint <= 0x7ff) {");
        writer.updateIndentation(1);
        writer.writeLine("out[0] = (char) (0xc0 | ((codepoint >> 6) & 0x1f));");
        writer.writeLine("out[1] = (char) (0x80 | (codepoint & 0x3f));");
        writer.writeLine("out[2] = '\\0';");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine("else if (codepoint <= 0xffff) {");
        writer.updateIndentation(1);
        writer.writeLine("out[0] = (char) (0xe0 | ((codepoint >> 12) & 0x0f));");
        writer.writeLine("out[1] = (char) (0x80 | ((codepoint >> 6) & 0x3f));");
        writer.writeLine("out[2] = (char) (0x80 | (codepoint & 0x3f));");
        writer.writeLine("out[3] = '\\0';");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine("else {");
        writer.updateIndentation(1);
        writer.writeLine("out[0] = (char) (0xf0 | ((codepoint >> 18) & 0x07));");
        writer.writeLine("out[1] = (char) (0x80 | ((codepoint >> 12) & 0x3f));");
        writer.writeLine("out[2] = (char) (0x80 | ((codepoint >> 6) & 0x3f));");
        writer.writeLine("out[3] = (char) (0x80 | (codepoint & 0x3f));");
        writer.writeLine("out[4] = '\\0';");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine();
        writer.writeLine("return out;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
        writer.writeLine();

        writer.writeLine("int main(int argc, char *argv[]) {");
        writer.updateIndentation(1);

        if (Platform.getOSType() == Platform.WINDOWS) {
            writer.writeLine("system(\"cmd.exe /c chcp 65001 > nul\");");
            writer.writeLine();
        }

        writer.writeLine(datatype + " memory[MEMORY_SIZE];");
        writer.writeLine("memset(memory, 0, MEMORY_SIZE);");
        writer.writeLine();
        writer.writeLine("int pointer = 0;");
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
                writer.writeLine("pointer = updatePointer(pointer, " + sum + ");");
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];
                writer.writeLine("memory[pointer] += " + sum + ";");
            }
            // handle output (.)
            else if (ch == '.') {
                writer.writeLine("printf(\"%s\", getSymbol(memory[pointer]));");
                writer.writeLine("fflush(stdout);");
            }
            // handle input (,)
            else if (ch == ',') {
                writer.writeLine("char ch;");
                writer.writeLine("scanf(\"%c\", &ch);");
                writer.writeLine("memory[pointer] = ch;\n");
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

        writer.writeLine();
        writer.writeLine("return 0;");
        writer.updateIndentation(-1);
        writer.writeLine("}");
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
