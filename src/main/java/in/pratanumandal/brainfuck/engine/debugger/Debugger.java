package in.pratanumandal.brainfuck.engine.debugger;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.component.TabData;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Alert;
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

    protected final ChangeListener<String> textChangeListener;

    protected Debugger(TabData tabData) {
        this.tabData = tabData;

        this.brackets = new HashMap<>();

        this.codeArea = tabData.getCodeArea();

        this.pause = new AtomicBoolean(false);

        this.kill = new AtomicBoolean(true);

        this.textChangeListener = (obs, oldVal, newVal) -> {
            this.code = tabData.getFileText();
            try {
                this.initializeBrackets();
            } catch (UnmatchedBracketException e) { }
        };
    }

    public void start() {
        this.code = tabData.getFileText();

        tabData.getDebugTerminal().reset();

        synchronized (this.kill) {
            this.kill.set(false);
        }

        this.clearMemory();

        try {
            this.initializeBrackets();
        } catch (UnmatchedBracketException e) { }

        this.codeArea.textProperty().addListener(textChangeListener);

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
                    Utils.throwUnmatchedBracketException(code, stack.pop() + 1);
                }
            }
            else if (i != -1 && (i < j || j == -1)) {
                stack.push(i);
                index = i + 1;
            }
            else if (j != -1) {
                if (stack.isEmpty()) {
                    Utils.throwUnmatchedBracketException(code, j + 1);
                }

                int k = stack.pop();
                brackets.put(k, j);
                brackets.put(j, k);
                index = j + 1;
            }
        }

        if (!stack.isEmpty()) {
            Utils.throwUnmatchedBracketException(code, stack.pop() + 1);
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

        if (join && this.thread != null && this.thread.isAlive()) {
            try {
                this.thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.tabData.getDebugTerminal().flush();

        this.codeArea.textProperty().removeListener(textChangeListener);

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

    protected void showUnmatchedBrackets(UnmatchedBracketException e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(Constants.APPLICATION_NAME);
            alert.setHeaderText("Debug Error");
            alert.setContentText(e.getMessage() + "\n\n");

            alert.initOwner(tabData.getTab().getTabPane().getScene().getWindow());
            alert.showAndWait();
        });
    }

}
