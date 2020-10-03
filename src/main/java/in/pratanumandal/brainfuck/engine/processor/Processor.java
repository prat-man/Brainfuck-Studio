package in.pratanumandal.brainfuck.engine.processor;

import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.util.Pair;
import org.fxmisc.richtext.CodeArea;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Processor implements Runnable {

    protected TabData tabData;

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

    public Processor(TabData tabData) {
        this.tabData = tabData;

        this.codeArea = tabData.getCodeArea();

        this.kill = new AtomicBoolean(true);
    }

    public void start() {
        this.code = tabData.getFileText();

        this.processed = new char[code.length()];
        this.jumps = new int[code.length()];

        synchronized (this.kill) {
            this.kill.set(false);
        }

        this.initializeJumps();

        thread = new Thread(this);
        thread.start();
    }

    private void initializeJumps() {
        Stack<Pair<Integer, Integer>> stack = new Stack<>();

        for (int i = 0, index = 0; i < this.code.length(); i++, index++) {
            // get one character
            Character ch = this.code.charAt(i);

            // create jumps for opening and closing square brackets [ and ]
            if (ch == '[') {
                Character ch2 = (i + 1) < this.code.length() ? this.code.charAt(i + 1) : 0;
                Character ch3 = (i + 2) < this.code.length() ? this.code.charAt(i + 2) : 0;
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
                    stack.push(new Pair<>(index, i));
                }
            }
            else if (ch == ']') {
                if (stack.isEmpty()) {
                    Utils.throwUnmatchedBracketException(code, i + 1);
                }

                // pop opening bracket and swap indexes in jump table
                int x = stack.pop().getKey();
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

        if (!stack.isEmpty()) {
            Utils.throwUnmatchedBracketException(code, stack.pop().getValue() + 1);
        }

        // strip ending null characters
        processed = new String(processed).trim().toCharArray();
        jumps = Utils.truncate(jumps, processed.length);
    }

    public void stop() {
        stop(true);
    }

    protected void stop(boolean join) {
        synchronized (this.kill) {
            this.kill.set(true);
        }

        if (join && this.thread != null && this.thread.isAlive()) {
            try {
                this.thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean isAlive() {
        return !this.kill.get();
    }

}
