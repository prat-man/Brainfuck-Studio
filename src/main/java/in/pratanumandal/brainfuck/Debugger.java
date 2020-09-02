package in.pratanumandal.brainfuck;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Slider;
import javafx.scene.input.ContextMenuEvent;
import org.fxmisc.richtext.CodeArea;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class Debugger implements Runnable {

    private TabData tabData;
    private Byte[] memory;

    private CodeArea codeArea;

    private String code;
    private final AtomicBoolean pause;

    private final AtomicBoolean kill;

    private Thread thread;

    private final EventHandler<ContextMenuEvent> consumeAllContextMenu = Event::consume;

    public Debugger(TabData tabData) {
        this.tabData = tabData;
        this.memory = new Byte[Constants.MEMORY_SIZE];

        this.codeArea = tabData.getCodeArea();

        this.pause = new AtomicBoolean(false);

        this.kill = new AtomicBoolean(true);
    }

    public void start() {
        this.code = tabData.getFileText();
        this.codeArea.setEditable(false);

        this.codeArea.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);

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

        thread = new Thread(this);
        thread.start();

        this.tabData.getResumeButton().setDisable(true);
        this.tabData.getPauseButton().setDisable(false);
        this.tabData.getStepButton().setDisable(false);
        this.tabData.getStopButton().setDisable(false);
        this.tabData.getCloseButton().setDisable(true);
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
        synchronized (this.kill) {
            this.kill.set(true);
        }

        synchronized (this.pause) {
            this.pause.set(false);
            this.pause.notify();
        }

        tabData.getDebugTerminal().release();

        try {
            this.thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        TableViewExtra<MemoryRow> tvX = new TableViewExtra(tabData.getTableView());

        int dataPointer = 0;
        int l = 0;

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
                pause();
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
                Platform.runLater(() -> {
                    Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                    memoryBlock.setData(Byte.toUnsignedInt(memory[finalDataPointer]));
                    tabData.getMemory().set(finalDataPointer, memoryBlock);
                });
            } else if (code.charAt(i) == '-') {
                memory[dataPointer]--;

                int finalDataPointer = dataPointer;
                Platform.runLater(() -> {
                    Memory memoryBlock = tabData.getMemory().get(finalDataPointer);
                    memoryBlock.setData(Byte.toUnsignedInt(memory[finalDataPointer]));
                    tabData.getMemory().set(finalDataPointer, memoryBlock);
                });
            } else if (code.charAt(i) == '.') {
                String text = String.valueOf((char) this.memory[dataPointer].intValue());
                Platform.runLater(() -> tabData.getDebugTerminal().write(text));
            } else if (code.charAt(i) == ',') {
                Character character = tabData.getDebugTerminal().readChar();
                memory[dataPointer] = character == null ? (byte) 0 : (byte) (int) character;
            } else if (code.charAt(i) == '[') {
                if (memory[dataPointer] == 0) {
                    i++;
                    while (l > 0 || code.charAt(i) != ']') {
                        if (code.charAt(i) == '[') l++;
                        if (code.charAt(i) == ']') l--;
                        i++;
                    }
                }
            } else if (code.charAt(i) == ']') {
                if (memory[dataPointer] != 0) {
                    i--;
                    while (l > 0 || code.charAt(i) != '[') {
                        if (code.charAt(i) == ']') l++;
                        if (code.charAt(i) == '[') l--;
                        i--;
                    }
                    i--;
                }
            }

            try {
                int delay =  (int) (debugSpeed.getMax() - debugSpeed.getValue() + debugSpeed.getMajorTickUnit());
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
