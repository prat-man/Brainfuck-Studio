package in.pratanumandal.brainfuck.engine.debugger;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.Memory;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;
import in.pratanumandal.brainfuck.gui.TableViewExtra;
import javafx.application.Platform;
import javafx.scene.control.Slider;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class Debugger16 extends Debugger {

    protected Short[] memory;

    public Debugger16(TabData tabData) {
        super(tabData);

        this.memory = new Short[Configuration.getMemorySize()];
    }

    @Override
    public void clearMemory() {
        Arrays.fill(this.memory, (short) 0);

        for (int i = 0; i < memory.length; i++) {
            tabData.getMemory().get(i).setData(Short.toUnsignedInt(memory[i]));
        }
        Platform.runLater(() -> tabData.getTableView().refresh());
    }

    @Override
    public void run() {

        AtomicReference<NotificationManager.Notification> notificationAtomicReference = new AtomicReference<>();
        Utils.runAndWait(() -> notificationAtomicReference.set(Utils.addNotification("File " + tabData.getTab().getText() + " debugging started")));
        NotificationManager.Notification notification = notificationAtomicReference.get();

        Slider debugSpeed = tabData.getDebugSpeed();
        TableViewExtra<Memory> tvX = new TableViewExtra<>(tabData.getTableView());

        int dataPointer = 0;

        for (int i = 0; i < code.length() && !this.kill.get(); i++) {
            synchronized (this.pause) {
                if (this.pause.get()) {
                    try {
                        this.pause.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

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
                    tabData.getDebugTerminal().write("\nError: Memory index out of bounds " + dataPointer + "\n");
                    this.stop(false);
                }
                else {
                    int finalDataPointer = dataPointer;
                    Platform.runLater(() -> {
                        int firstVisRowIndex = tvX.getFirstVisibleIndex();
                        int lastVisRowIndex = tvX.getLastVisibleIndex();
                        if (firstVisRowIndex > finalDataPointer || lastVisRowIndex < finalDataPointer) {
                            tabData.getTableView().scrollTo(finalDataPointer);
                        }
                        tabData.getTableView().getSelectionModel().select(finalDataPointer);
                    });
                }
            } else if (code.charAt(i) == '<') {
                dataPointer--;
                if (dataPointer < 0) {
                    tabData.getDebugTerminal().write("\nError: Memory index out of bounds " + dataPointer + "\n");
                    this.stop(false);
                }
                else {
                    int finalDataPointer = dataPointer;
                    Platform.runLater(() -> {
                        int firstVisRowIndex = tvX.getFirstVisibleIndex();
                        int lastVisRowIndex = tvX.getLastVisibleIndex();
                        if (firstVisRowIndex > finalDataPointer || lastVisRowIndex < finalDataPointer) {
                            tabData.getTableView().scrollTo(finalDataPointer);
                        }
                        tabData.getTableView().getSelectionModel().select(finalDataPointer);
                    });
                }
            } else if (code.charAt(i) == '+') {
                memory[dataPointer]++;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Short.toUnsignedInt(memory[finalDataPointer]));
                Platform.runLater(() ->  tabData.getMemory().set(finalDataPointer, memoryBlock));
            } else if (code.charAt(i) == '-') {
                memory[dataPointer]--;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Short.toUnsignedInt(memory[finalDataPointer]));
                Platform.runLater(() ->  tabData.getMemory().set(finalDataPointer, memoryBlock));
            } else if (code.charAt(i) == '.') {
                int codePoint = this.memory[dataPointer].intValue();
                if (codePoint < 0) codePoint += 256;
                String text = String.valueOf((char) codePoint);
                tabData.getDebugTerminal().write(text);
            } else if (code.charAt(i) == ',') {
                Character character = tabData.getDebugTerminal().readChar();
                memory[dataPointer] = character == null ? (short) 0 : (short) (int) character;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Short.toUnsignedInt(memory[finalDataPointer]));
                Platform.runLater(() ->  tabData.getMemory().set(finalDataPointer, memoryBlock));
            } else if (code.charAt(i) == '[') {
                if (memory[dataPointer] == 0) {
                    i = brackets.get(i);
                }
            } else if (code.charAt(i) == ']') {
                if (memory[dataPointer] != 0) {
                    i = brackets.get(i);
                }
            }

            try {
                int delay = (int) (debugSpeed.getMax() - debugSpeed.getValue() + debugSpeed.getMajorTickUnit());
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
