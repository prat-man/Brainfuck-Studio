package in.pratanumandal.brainfuck.engine;

import in.pratanumandal.brainfuck.gui.TabData;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.gui.TableViewExtra;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Slider;
import javafx.scene.input.ContextMenuEvent;
import org.fxmisc.richtext.CodeArea;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public class Debugger implements Runnable {

    private TabData tabData;

    private Byte[] memory;
    private Map<Integer, Integer> brackets;

    private CodeArea codeArea;

    private String code;
    private final AtomicBoolean pause;

    private final AtomicBoolean kill;

    private Thread thread;

    private final EventHandler<ContextMenuEvent> consumeAllContextMenu = Event::consume;

    public Debugger(TabData tabData) {
        this.tabData = tabData;

        this.memory = new Byte[Constants.MEMORY_SIZE];
        this.brackets = new HashMap<>();

        this.codeArea = tabData.getCodeArea();

        this.pause = new AtomicBoolean(false);

        this.kill = new AtomicBoolean(true);
    }

    public void start() {
        this.code = tabData.getFileText();

        tabData.getDebugTerminal().clear();
        tabData.getDebugTerminal().flush();

        synchronized (this.kill) {
            this.kill.set(false);
        }

        Arrays.fill(this.memory, (byte) 0);

        for (int i = 0; i < memory.length; i++) {
            tabData.getMemory().get(i).setData(Byte.toUnsignedInt(memory[i]));
        }
        Platform.runLater(() -> tabData.getTableView().refresh());

        try {
            this.initializeBrackets();
        } catch (UnmatchedLoopException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(Constants.APPLICATION_NAME);
            alert.setHeaderText("Debug Error");
            alert.setContentText(e.getMessage() + "\n\n");

            alert.initOwner(tabData.getTab().getTabPane().getScene().getWindow());
            alert.showAndWait();

            return;
        }

        this.codeArea.setEditable(false);

        this.codeArea.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);

        thread = new Thread(this);
        thread.start();

        this.tabData.getResumeButton().setDisable(true);
        this.tabData.getPauseButton().setDisable(false);
        this.tabData.getStepButton().setDisable(false);
        this.tabData.getStopButton().setDisable(false);
        this.tabData.getCloseButton().setDisable(true);
    }

    private void initializeBrackets() {
        this.brackets.clear();

        Stack<Integer> stack = new Stack<>();
        int index = 0;

        while (index < this.code.length()) {
            int i = this.code.indexOf("[", index);
            int j = this.code.indexOf("]", index);

            if (i == -1 && j == -1) {
                if (stack.isEmpty()) break;
                else throw new UnmatchedLoopException("Unmatched loop at position " + (stack.pop() + 1));
            }
            else if (i != -1 && (i < j || j == -1)) {
                stack.push(i);
                index = i + 1;
            }
            else if (j != -1) {
                if (stack.isEmpty()) throw new UnmatchedLoopException("Unmatched loop at position " + (j + 1));

                int k = stack.pop();
                brackets.put(k, j);
                brackets.put(j, k);
                index = j + 1;
            }
        }

        if (!stack.isEmpty()) throw new UnmatchedLoopException("Unmatched loop at position " + (stack.pop() + 1));
    }

    public void pause() {
        synchronized (this.pause) {
            this.pause.set(true);
        }

        tabData.getResumeButton().setDisable(false);
        tabData.getPauseButton().setDisable(true);
    }

    public void resume() {
        synchronized (this.pause) {
            this.pause.set(false);
            this.pause.notify();
        }

        tabData.getResumeButton().setDisable(true);
        tabData.getPauseButton().setDisable(false);
    }

    public void step() {
        synchronized (this.pause) {
            this.pause.set(true);
            this.pause.notify();
        }

        tabData.getResumeButton().setDisable(false);
        tabData.getPauseButton().setDisable(true);
    }

    public void stop() {
        stop(true);
    }

    private void stop(boolean join) {
        synchronized (this.kill) {
            this.kill.set(true);
        }

        synchronized (this.pause) {
            this.pause.set(false);
            this.pause.notify();
        }

        tabData.getDebugTerminal().release();

        if (join && this.thread != null && this.thread.isAlive()) {
            try {
                this.thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.codeArea.setEditable(true);

        this.codeArea.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);

        this.tabData.getResumeButton().setDisable(true);
        this.tabData.getPauseButton().setDisable(true);
        this.tabData.getStepButton().setDisable(true);
        this.tabData.getStopButton().setDisable(true);
        this.tabData.getCloseButton().setDisable(false);
    }

    public Boolean isAlive() {
        return !this.kill.get();
    }

    @Override
    public void run() {

        Slider debugSpeed = tabData.getDebugSpeed();
        TableViewExtra<Memory> tvX = new TableViewExtra(tabData.getTableView());

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

            if (ch == '~') {
                this.pause();
            } else if (ch == '>') {
                dataPointer = (dataPointer == Constants.MEMORY_SIZE - 1) ? 0 : dataPointer + 1;

                int finalDataPointer = dataPointer;
                Platform.runLater(() -> {
                    int firstVisRowIndex = tvX.getFirstVisibleIndex();
                    int lastVisRowIndex = tvX.getLastVisibleIndex();
                    if (firstVisRowIndex > finalDataPointer || lastVisRowIndex < finalDataPointer) {
                        tabData.getTableView().scrollTo(finalDataPointer);
                    }
                    tabData.getTableView().getSelectionModel().select(finalDataPointer);
                });
            } else if (code.charAt(i) == '<') {
                dataPointer = (dataPointer == 0) ? Constants.MEMORY_SIZE - 1 : dataPointer - 1;

                int finalDataPointer = dataPointer;
                Platform.runLater(() -> {
                    int firstVisRowIndex = tvX.getFirstVisibleIndex();
                    int lastVisRowIndex = tvX.getLastVisibleIndex();
                    if (firstVisRowIndex > finalDataPointer || lastVisRowIndex < finalDataPointer) {
                        tabData.getTableView().scrollTo(finalDataPointer);
                    }
                    tabData.getTableView().getSelectionModel().select(finalDataPointer);
                });
            } else if (code.charAt(i) == '+') {
                memory[dataPointer]++;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Byte.toUnsignedInt(memory[finalDataPointer]));
                Platform.runLater(() ->  tabData.getMemory().set(finalDataPointer, memoryBlock));
            } else if (code.charAt(i) == '-') {
                memory[dataPointer]--;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Byte.toUnsignedInt(memory[finalDataPointer]));
                Platform.runLater(() ->  tabData.getMemory().set(finalDataPointer, memoryBlock));
            } else if (code.charAt(i) == '.') {
                String text = String.valueOf((char) this.memory[dataPointer].intValue());
                Platform.runLater(() -> tabData.getDebugTerminal().write(text));
            } else if (code.charAt(i) == ',') {
                Character character = tabData.getDebugTerminal().readChar();
                memory[dataPointer] = character == null ? (byte) 0 : (byte) (int) character;

                int finalDataPointer = dataPointer;
                Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                memoryBlock.setData(Byte.toUnsignedInt(memory[finalDataPointer]));
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
                int delay =  (int) (debugSpeed.getMax() - debugSpeed.getValue() + debugSpeed.getMajorTickUnit());
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.stop(false);

    }

}
