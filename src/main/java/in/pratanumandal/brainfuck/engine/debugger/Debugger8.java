package in.pratanumandal.brainfuck.engine.debugger;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.Memory;
import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.application.Platform;
import javafx.scene.control.Slider;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class Debugger8 extends Debugger {

    protected Byte[] memory;

    public Debugger8(TabData tabData) {
        super(tabData);

        this.memory = new Byte[Configuration.getMemorySize()];
    }

    @Override
    public void clearMemory() {
        Arrays.fill(this.memory, (byte) 0);

        for (int i = 0; i < memory.length; i++) {
            tabData.getMemory().get(i).setData(Byte.toUnsignedInt(memory[i]));
        }
        Platform.runLater(() -> tabData.getTableView().refresh());
    }

    @Override
    public void run() {

        AtomicReference<NotificationManager.Notification> notificationAtomicReference = new AtomicReference<>();
        Utils.runAndWait(() -> notificationAtomicReference.set(Utils.addNotification("File " + tabData.getTab().getText() + " debugging started")));
        NotificationManager.Notification notification = notificationAtomicReference.get();

        Slider debugSpeed = tabData.getDebugSpeed();

        int dataPointer = 0;

        for (int i = 0; i < code.length() && !this.kill.get(); i++) {
            int finalI = i;
            Platform.runLater(() -> {
                this.codeArea.selectRange(finalI, finalI + 1);
                this.codeArea.requestFollowCaret();
            });

            char ch = code.charAt(i);

            if (ch == '~' && tabData.getDebugBreakpointButton().isSelected()) {
                this.pause();
            } else if (ch == '>') {
                dataPointer++;
                if (dataPointer >= this.memory.length) {
                    tabData.getDebugTerminal().writeError("\nError: Memory index out of bounds " + dataPointer + "\n");
                    this.stop(false);
                }
                else {
                    int finalDataPointer = dataPointer;
                    Platform.runLater(() -> {
                        tabData.getTableViewExtra().scrollToIndex(finalDataPointer);
                        tabData.getTableView().getSelectionModel().select(finalDataPointer);
                    });
                }
            } else if (ch == '<') {
                dataPointer--;
                if (dataPointer < 0) {
                    tabData.getDebugTerminal().writeError("\nError: Memory index out of bounds " + dataPointer + "\n");
                    this.stop(false);
                }
                else {
                    int finalDataPointer = dataPointer;
                    Platform.runLater(() -> {
                        tabData.getTableViewExtra().scrollToIndex(finalDataPointer);
                        tabData.getTableView().getSelectionModel().select(finalDataPointer);
                    });
                }
            } else if (ch == '+') {
                memory[dataPointer]++;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Byte.toUnsignedInt(memory[finalDataPointer]));
                Platform.runLater(() ->  tabData.getMemory().set(finalDataPointer, memoryBlock));
            } else if (ch == '-') {
                memory[dataPointer]--;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Byte.toUnsignedInt(memory[finalDataPointer]));
                Platform.runLater(() ->  tabData.getMemory().set(finalDataPointer, memoryBlock));
            } else if (ch == '.') {
                int codePoint = this.memory[dataPointer].intValue();
                if (codePoint < 0) codePoint += 256;
                String text = String.valueOf((char) codePoint);
                tabData.getDebugTerminal().write(text);
            } else if (ch == ',') {
                Character character = tabData.getDebugTerminal().readChar();
                memory[dataPointer] = character == null ? (byte) 0 : (byte) (int) character;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Byte.toUnsignedInt(memory[finalDataPointer]));
                Platform.runLater(() ->  tabData.getMemory().set(finalDataPointer, memoryBlock));
            } else if (ch == '[') {
                if (memory[dataPointer] == 0) {
                    Integer other = brackets.get(i);
                    try {
                        if (other == null) Utils.throwUnmatchedBracketException(code, i + 1);
                        else i = other;
                    } catch (UnmatchedBracketException e) {
                        showUnmatchedBrackets(e);
                        this.kill.set(true);
                    }
                }
            } else if (ch == ']') {
                if (memory[dataPointer] != 0) {
                    Integer other = brackets.get(i);
                    try {
                        if (other == null) Utils.throwUnmatchedBracketException(code, i + 1);
                        else i = other;
                    } catch (UnmatchedBracketException e) {
                        showUnmatchedBrackets(e);
                        this.kill.set(true);
                    }
                }
            }

            try {
                int delay = (int) (debugSpeed.getMax() - debugSpeed.getValue() + debugSpeed.getMajorTickUnit());
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (this.pause) {
                if (this.pause.get()) {
                    try {
                        this.pause.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        Platform.runLater(() -> notification.close());

        if (this.kill.get()) {
            Platform.runLater(() -> Utils.addNotification("File " + tabData.getTab().getText() + " debugging terminated"));
        }
        else {
            Platform.runLater(() -> Utils.addNotification("File " + tabData.getTab().getText() + " debugging finished"));
        }

        this.stop(false);

    }

}
