package in.pratanumandal.brainfuck.engine.debugger;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.TabData;
import in.pratanumandal.brainfuck.common.Constants;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.input.ContextMenuEvent;
import org.fxmisc.richtext.CodeArea;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Debugger implements Runnable {

    protected TabData tabData;

    protected Map<Integer, Integer> brackets;

    protected CodeArea codeArea;

    protected String code;

    protected final AtomicBoolean pause;
    protected final AtomicBoolean kill;

    protected Thread thread;

    protected final EventHandler<ContextMenuEvent> consumeAllContextMenu = Event::consume;

    protected Debugger(TabData tabData) {
        this.tabData = tabData;

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

        this.clearMemory();

        try {
            this.initializeBrackets();
        } catch (UnmatchedBracketException e) {
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

        this.tabData.getDebugResumeButton().setDisable(true);
        this.tabData.getDebugPauseButton().setDisable(false);
        this.tabData.getDebugStepButton().setDisable(false);
        this.tabData.getDebugStopButton().setDisable(false);
        this.tabData.getDebugCloseButton().setDisable(true);
    }

    protected abstract void clearMemory();

    private void initializeBrackets() {
        this.brackets.clear();

        Stack<Integer> stack = new Stack<>();
        int index = 0;

        while (index < this.code.length()) {
            int i = this.code.indexOf("[", index);
            int j = this.code.indexOf("]", index);

            if (i == -1 && j == -1) {
                if (stack.isEmpty()) break;
                else {
                    int pos = stack.pop() + 1;
                    String codeSlice = code.substring(0, pos - 1);
                    int row = Utils.countNewlines(codeSlice);
                    int col = Utils.calculateColumn(codeSlice) + 1;
                    throw new UnmatchedBracketException("Unmatched bracket at position " + pos + " [" + row + " : " + col + "]");
                }
            }
            else if (i != -1 && (i < j || j == -1)) {
                stack.push(i);
                index = i + 1;
            }
            else if (j != -1) {
                if (stack.isEmpty()) {
                    int pos = j + 1;
                    String codeSlice = code.substring(0, pos - 1);
                    int row = Utils.countNewlines(codeSlice);
                    int col = Utils.calculateColumn(codeSlice) + 1;
                    throw new UnmatchedBracketException("Unmatched bracket at position " + pos + " [" + row + " : " + col + "]");
                }

                int k = stack.pop();
                brackets.put(k, j);
                brackets.put(j, k);
                index = j + 1;
            }
        }

        if (!stack.isEmpty()) {
            int pos = stack.pop() + 1;
            String codeSlice = code.substring(0, pos - 1);
            int row = Utils.countNewlines(codeSlice);
            int col = Utils.calculateColumn(codeSlice) + 1;
            throw new UnmatchedBracketException("Unmatched bracket at position " + pos + " [" + row + " : " + col + "]");
        }
    }

    public void pause() {
        synchronized (this.pause) {
            this.pause.set(true);
        }

        tabData.getDebugResumeButton().setDisable(false);
        tabData.getDebugPauseButton().setDisable(true);
    }

    public void resume() {
        synchronized (this.pause) {
            this.pause.set(false);
            this.pause.notify();
        }

        tabData.getDebugResumeButton().setDisable(true);
        tabData.getDebugPauseButton().setDisable(false);
    }

    public void step() {
        synchronized (this.pause) {
            this.pause.set(true);
            this.pause.notify();
        }

        tabData.getDebugResumeButton().setDisable(false);
        tabData.getDebugPauseButton().setDisable(true);
    }

    public void stop() {
        stop(true);
    }

    protected void stop(boolean join) {
        synchronized (this.kill) {
            this.kill.set(true);
        }

        synchronized (this.pause) {
            this.pause.set(false);
            this.pause.notify();
        }

        this.tabData.getDebugTerminal().release();
        this.tabData.getDebugTerminal().flush();

        if (join && this.thread != null && this.thread.isAlive()) {
            try {
                this.thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.codeArea.setEditable(true);

        this.codeArea.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);

        this.tabData.getDebugResumeButton().setDisable(true);
        this.tabData.getDebugPauseButton().setDisable(true);
        this.tabData.getDebugStepButton().setDisable(true);
        this.tabData.getDebugStopButton().setDisable(true);
        this.tabData.getDebugCloseButton().setDisable(false);
    }

    public Boolean isAlive() {
        return !this.kill.get();
    }

    public static Debugger getDebugger(TabData tabData) {
        Integer cellSize = Configuration.getCellSize();

        if (cellSize == 8) {
            return new Debugger8(tabData);
        }
        else if (cellSize == 16) {
            return new Debugger16(tabData);
        }

        return null;
    }

}
