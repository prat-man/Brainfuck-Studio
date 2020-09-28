package in.pratanumandal.brainfuck.engine;

import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.TabData;
import in.pratanumandal.brainfuck.common.Constants;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.input.ContextMenuEvent;
import org.fxmisc.richtext.CodeArea;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Processor implements Runnable {

    protected TabData tabData;

    protected Byte[] memory;

    protected CodeArea codeArea;

    protected String code;

    protected char[] processed;
    protected int[] jumps;

    protected final AtomicBoolean kill;

    protected Thread thread;

    public static final Integer NO_JUMP = 0;
    public static final Character SET_ZERO = '!';
    public static final Character SCAN_ZERO_LEFT = '@';
    public static final Character SCAN_ZERO_RIGHT = '#';
    public static final Character ADDRESS = '$';
    public static final Character DATA = '%';

    public static boolean isOperator(Character ch) {
        return "<>+-,.[]".indexOf(ch) >= 0;
    }

    private final EventHandler<ContextMenuEvent> consumeAllContextMenu = Event::consume;

    public Processor(TabData tabData) {
        this.tabData = tabData;

        this.memory = new Byte[Constants.MEMORY_SIZE];

        this.codeArea = tabData.getCodeArea();

        this.kill = new AtomicBoolean(true);
    }

    public void start() {
        this.code = tabData.getFileText();

        this.processed = new char[code.length()];
        this.jumps = new int[code.length()];

        tabData.getInterpretTerminal().clear();
        tabData.getInterpretTerminal().flush();

        synchronized (this.kill) {
            this.kill.set(false);
        }

        Arrays.fill(this.memory, (byte) 0);

        for (int i = 0; i < memory.length; i++) {
            tabData.getMemory().get(i).setData(Byte.toUnsignedInt(memory[i]));
        }
        Platform.runLater(() -> tabData.getTableView().refresh());

        try {
            this.initializeJumps();
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

        this.tabData.getInterpretStopButton().setDisable(false);
        this.tabData.getInterpretCloseButton().setDisable(true);
    }

    private void initializeJumps() {
        Stack<Integer> stack = new Stack<>();

        for (int i = 0, index = 0; i < this.code.length(); i++, index++) {
            // get one character
            Character ch = this.code.charAt(i);

            // create jumps for opening and closing square brackets [ and ]
            if (ch == '[') {
                Character ch2 = (i + 1) < this.code.length() ? this.code.charAt(i + 1) : null;
                Character ch3 = (i + 2) < this.code.length() ? this.code.charAt(i + 2) : null;
                if (ch2 == '-' && ch3 == ']') {
                    // optimize [-] to set(0)
                    processed[index] = SET_ZERO;
                    i += 2;
                }
                else if (ch2 == '<' && ch3 == ']') {
                    // optimize [<] to scan_left(0)
                    processed[index] = SCAN_ZERO_LEFT;
                    i += 2;
                }
                else if (ch2 == '>' && ch3 == ']') {
                    // optimize [>] to scan_right(0)
                    processed[index] = SCAN_ZERO_RIGHT;
                    i += 2;
                }
                else {
                    // push opening bracket [ to stack
                    processed[index] = ch;
                    stack.push(index);
                }
            }
            else if (ch == ']') {
                // pop opening bracket and swap indexes in jump table
                int x = stack.pop();
                jumps[x] = index;
                jumps[index] = x;
                processed[index] = ch;
            }

            // compact and jump for > and <
            else if (ch == '>' || ch == '<') {
                int sum = 0;

                if (ch == '>') sum++;
                else sum--;

                while (++i < this.code.length()) {
                    if (this.code.charAt(i) == '>') {
                        sum++;
                    }
                    else if (this.code.charAt(i) == '<') {
                        sum--;
                    }
                    else if (isOperator(this.code.charAt(i))) {
                        break;
                    }
                }
                i--;

                // optimize out address operations if sum is zero
                if (sum == 0) {
                    index--;
                    continue;
                }

                processed[index] = ADDRESS;
                jumps[index] = sum;
            }

            // compact and jump for + and -
            else if (ch == '+' || ch == '-') {
                int sum = 0;

                if (ch == '+') sum++;
                else sum--;

                while (++i < this.code.length()) {
                    if (this.code.charAt(i) == '+') {
                        sum++;
                    }
                    else if (this.code.charAt(i) == '-') {
                        sum--;
                    }
                    else if (isOperator(this.code.charAt(i))) {
                        break;
                    }
                }
                i--;

                // optimize out data operations if sum is zero
                // or next operator is an input operation
                if (sum == 0 || ((i + 1) < this.code.length() && this.code.charAt(i + 1) == ',')) {
                    index--;
                    continue;
                }

                processed[index] = DATA;
                jumps[index] = sum;
            }

            // input or output no jump
            else if (ch == ',' || ch == '.') {
                processed[index] = ch;
            }

            // for everything else, do not include in pre-processed source
            else {
                index--;
            }
        }
    }

    public void stop() {
        stop(true);
    }

    protected void stop(boolean join) {
        synchronized (this.kill) {
            this.kill.set(true);
        }

        tabData.getInterpretTerminal().release();

        if (join && this.thread != null && this.thread.isAlive()) {
            try {
                this.thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.codeArea.setEditable(true);

        this.codeArea.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);

        this.tabData.getInterpretStopButton().setDisable(true);
        this.tabData.getInterpretCloseButton().setDisable(false);
    }

    public Boolean isAlive() {
        return !this.kill.get();
    }

}
