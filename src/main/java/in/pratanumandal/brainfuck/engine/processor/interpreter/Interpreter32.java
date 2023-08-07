package in.pratanumandal.brainfuck.engine.processor.interpreter;

import in.pratanumandal.brainfuck.common.CharacterUtils;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import in.pratanumandal.brainfuck.os.windows.WindowsUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class Interpreter32 extends Interpreter {

    protected Integer[] memory;

    public Interpreter32(TabData tabData) {
        super(tabData);

        this.memory = new Integer[this.memorySize];
    }

    @Override
    public void start() {
        tabData.getInterpretTerminal().reset();

        Arrays.fill(this.memory, 0);

        try {
            super.start();
        } catch (UnmatchedBracketException e) {
            Platform.runLater(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle(Constants.APPLICATION_NAME);
                error.setHeaderText("Interpreter Error");
                error.setContentText(e.getMessage() + "\n\n");

                WindowsUtils.setStageStyle((Stage) error.getDialogPane().getScene().getWindow());

                error.initOwner(tabData.getTab().getTabPane().getScene().getWindow());
                error.showAndWait();
            });

            return;
        }

        this.tabData.getInterpretStopButton().setDisable(false);
        this.tabData.getInterpretCloseButton().setDisable(true);
    }

    @Override
    protected void stop(boolean join) {
        synchronized (this.kill) {
            this.kill.set(true);
        }

        this.tabData.getInterpretTerminal().release();

        super.stop(join);

        this.tabData.getInterpretTerminal().flush();

        this.tabData.getInterpretStopButton().setDisable(true);
        this.tabData.getInterpretCloseButton().setDisable(false);
    }

    @Override
    public void run() {
        AtomicReference<NotificationManager.Notification> notificationAtomicReference = new AtomicReference<>();
        Utils.runAndWait(() -> notificationAtomicReference.set(Utils.addNotification(tabData.getTab().getText() + " execution started")));
        NotificationManager.Notification notification = notificationAtomicReference.get();

        // start time
        long startTime = System.nanoTime();

        int dataPointer = 0;

        for (int i = 0; i < processed.length && !this.kill.get(); i++) {
            char ch = processed[i];

            // handle pointer movement (> and <)
            if (ch == ADDRESS) {
                int sum = jumps[i];
                dataPointer += sum;

                if (this.wrapMemory) {
                    if (dataPointer < 0) dataPointer += this.memorySize;
                    else if (dataPointer >= this.memorySize) dataPointer -= this.memorySize;
                }
                else if (dataPointer < 0 || dataPointer >= this.memorySize) {
                    tabData.getInterpretTerminal().writeError("\nError: Memory index out of bounds " + dataPointer + "\n");
                    this.stop(false);
                }
            }
            // handle value update (+ and -)
            else if (ch == DATA) {
                int sum = jumps[i];
                memory[dataPointer] = memory[dataPointer] + sum;
            }
            // handle output (.)
            else if (ch == '.') {
                String symbol = CharacterUtils.getSymbol(this.memory[dataPointer]);
                tabData.getInterpretTerminal().write(symbol);
            }
            // handle input (,)
            else if (ch == ',') {
                Character character = tabData.getInterpretTerminal().readChar();
                memory[dataPointer] = character == null ? 0 : (int) character;
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
        tabData.getInterpretTerminal().writeMessage("\n\n");
        tabData.getInterpretTerminal().writeMessage("--------------------------------------------------------------------------------\n");

        Platform.runLater(() -> notification.close());

        if (this.kill.get()) {
            tabData.getInterpretTerminal().writeMessage("Execution terminated; Runtime " + durationStr + "\n");
            Platform.runLater(() -> Utils.addNotification(tabData.getTab().getText() + " execution terminated"));
        }
        else {
            tabData.getInterpretTerminal().writeMessage("Execution completed in " + durationStr + "\n");
            Platform.runLater(() -> Utils.addNotification(tabData.getTab().getText() + " execution finished"));
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
        for (int i = this.memorySize - 1; i > position; i--) {
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
        for (int i = position; i < this.memorySize; i++) {
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
