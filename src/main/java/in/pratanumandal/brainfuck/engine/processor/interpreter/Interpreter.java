package in.pratanumandal.brainfuck.engine.processor.interpreter;

import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.processor.Processor;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.application.Platform;

import java.util.Arrays;

public class Interpreter extends Processor {

    protected Byte[] memory;

    //private final EventHandler<ContextMenuEvent> consumeAllContextMenu = Event::consume;

    public Interpreter(TabData tabData) {
        super(tabData);

        this.memory = new Byte[Constants.MEMORY_SIZE];
    }

    @Override
    public void start() {
        tabData.getInterpretTerminal().clear();
        tabData.getInterpretTerminal().flush();

        Arrays.fill(this.memory, (byte) 0);

        for (int i = 0; i < memory.length; i++) {
            tabData.getMemory().get(i).setData(Byte.toUnsignedInt(memory[i]));
        }
        Platform.runLater(() -> tabData.getTableView().refresh());

        //this.codeArea.setEditable(false);

        //this.codeArea.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);

        super.start();

        this.tabData.getInterpretStopButton().setDisable(false);
        this.tabData.getInterpretCloseButton().setDisable(true);
    }

    @Override
    protected void stop(boolean join) {
        synchronized (this.kill) {
            this.kill.set(true);
        }

        tabData.getInterpretTerminal().release();

        super.stop(join);

        //this.codeArea.setEditable(true);

        //this.codeArea.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);

        this.tabData.getInterpretStopButton().setDisable(true);
        this.tabData.getInterpretCloseButton().setDisable(false);
    }

    @Override
    public void run() {

        // start time
        long startTime = System.nanoTime();

        int dataPointer = 0;

        for (int i = 0; i < processed.length && !this.kill.get(); i++) {
            char ch = processed[i];

            // handle pointer movement (> and <)
            if (ch == ADDRESS) {
                int sum = jumps[i];
                dataPointer += sum;
                if (dataPointer < 0 || dataPointer >= this.memory.length) {
                    tabData.getInterpretTerminal().write("\nError: Memory index out of bounds " + dataPointer + "\n");
                    this.stop(false);
                }
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];
                memory[dataPointer] = (byte) (memory[dataPointer] + sum);
            }
            // handle output (.)
            else if (ch == '.') {
                int codePoint = this.memory[dataPointer].intValue();
                if (codePoint < 0) codePoint += 256;
                String text = String.valueOf((char) codePoint);
                tabData.getInterpretTerminal().write(text);
            }
            // handle input (,)
            else if (ch == ',') {
                Character character = tabData.getInterpretTerminal().readChar();
                memory[dataPointer] = character == null ? (byte) 0 : (byte) (int) character;
            }
            // handle [-]
            else if (ch == SET_ZERO) {
                memory[dataPointer] = 0;
            }
            // handle [<]
            else if (ch == SCAN_ZERO_LEFT) {
                dataPointer = this.findZeroLeft(dataPointer);
            }
            // handle [>]
            else if (ch == SCAN_ZERO_RIGHT) {
                dataPointer = this.findZeroRight(dataPointer);
            }
            // handle loop opening ([)
            else if (ch == '[') {
                if (memory[dataPointer] == 0) {
                    i = jumps[i];
                }
            }
            // handle loop closing (])
            else if (ch == ']') {
                if (memory[dataPointer] != 0) {
                    i = jumps[i];
                }
            }
        }

        // stop time
        long stopTime = System.nanoTime();

        // execution duration
        long duration = stopTime - startTime;
        String durationStr = Utils.nanoToBestFitTimeUnits(duration);

        // print the execution time
        tabData.getInterpretTerminal().write("\n\n");
        tabData.getInterpretTerminal().write("--------------------------------------------------------------------------------\n");

        if (this.kill.get()) {
            tabData.getInterpretTerminal().write("Execution terminated; Runtime " + durationStr + "\n");
            Platform.runLater(() -> Utils.addNotification("File " + tabData.getTab().getText() + " execution terminated"));
        }
        else {
            tabData.getInterpretTerminal().write("Execution completed in " + durationStr + "\n");
            Platform.runLater(() -> Utils.addNotification("File " + tabData.getTab().getText() + " finished execution"));
        }

        this.stop(false);

    }

    /**
     * Find first zero in memory at or to the left of position.
     */
    private int findZeroLeft(int position) {
        for (int i = position; i >= 0; i--) {
            if (memory[i] == 0) {
                return i;
            }
        }
        for (int i = this.memory.length - 1; i > position; i--) {
            if (memory[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find first zero in memory at or to the right of position.
     */
    private int findZeroRight(int position) {
        for (int i = position; i < this.memory.length; i++) {
            if (memory[i] == 0) {
                return i;
            }
        }
        for (int i = 0; i < position; i++) {
            if (memory[i] == 0) {
                return i;
            }
        }
        return -1;
    }

}
