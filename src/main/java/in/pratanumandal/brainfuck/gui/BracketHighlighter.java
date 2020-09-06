package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.common.Utils;
import javafx.application.Platform;
import javafx.event.Event;
import org.fxmisc.richtext.NavigationActions;
import org.fxmisc.richtext.model.GenericEditableStyledDocument;

import java.util.*;

public class BracketHighlighter {

    private final TabData tabData;

    private Map<Integer, Integer> brackets;

    private List<BracketPair> bracketPairs;

    public BracketHighlighter(TabData tabData) {
        this.tabData = tabData;

        this.brackets = new HashMap<>();
        this.bracketPairs = new ArrayList<>();

        this.tabData.getCodeArea().addTextInsertionListener((start, end, text) -> clearBracket());
        this.tabData.getCodeArea().textProperty().addListener((obs, oldVal, newVal) -> initializeBrackets(newVal));
        this.tabData.getCodeArea().caretPositionProperty().addListener((obs, oldVal, newVal) -> highlightBracket(newVal));
    }

    private synchronized void initializeBrackets(String code) {

        this.brackets.clear();

        Stack<Integer> stack = new Stack<>();
        int index = 0;

        while (index < code.length()) {
            int i = code.indexOf("[", index);
            int j = code.indexOf("]", index);

            if (i == -1 && j == -1) {
                break;
            } else if (i != -1 && (i < j || j == -1)) {
                stack.push(i);
                index = i + 1;
            } else if (j != -1) {
                if (!stack.isEmpty()) {
                    int k = stack.pop();
                    brackets.put(k, j);
                    brackets.put(j, k);
                }
                index = j + 1;
            }
        }

    }

    private synchronized void highlightBracket(int newVal) {

        String prevChar = (newVal > 0) ? this.tabData.getCodeArea().getText(newVal - 1, newVal) : "";
        if (prevChar.equals("[") || prevChar.equals("]")) newVal--;

        Iterator<BracketPair> iterator = this.bracketPairs.iterator();
        while (iterator.hasNext()) {
            BracketPair pair = iterator.next();

            if (pair.start < this.tabData.getCodeArea().getLength()) {
                String text = this.tabData.getCodeArea().getText(pair.start, pair.start + 1);
                if (text.equals("[") || text.equals("]")) {
                    List<String> styleList = new ArrayList<>();
                    styleList.add("loop");
                    this.tabData.getCodeArea().setStyle(pair.start, pair.start + 1, styleList);
                }
            }
            if (pair.end < this.tabData.getCodeArea().getLength()) {
                String text = this.tabData.getCodeArea().getText(pair.end, pair.end + 1);
                if (text.equals("[") || text.equals("]")) {
                    List<String> styleList = new ArrayList<>();
                    styleList.add("loop");
                    this.tabData.getCodeArea().setStyle(pair.end, pair.end + 1, styleList);
                }
            }

            iterator.remove();
        }

        Integer other = this.brackets.get(newVal);

        if (other != null) {
            BracketPair pair = new BracketPair(newVal, other);

            if (pair.start < this.tabData.getCodeArea().getLength()) {
                String text = this.tabData.getCodeArea().getText(pair.start, pair.start + 1);
                if (text.equals("[") || text.equals("]")) {
                    List<String> styleList = new ArrayList<>();
                    styleList.add("loop");
                    styleList.add("match");
                    this.tabData.getCodeArea().setStyle(pair.start, pair.start + 1, styleList);
                }
            }
            if (pair.end < this.tabData.getCodeArea().getLength()) {
                String text = this.tabData.getCodeArea().getText(pair.end, pair.end + 1);
                if (text.equals("[") || text.equals("]")) {
                    List<String> styleList = new ArrayList<>();
                    styleList.add("loop");
                    styleList.add("match");
                    this.tabData.getCodeArea().setStyle(pair.end, pair.end + 1, styleList);
                }
            }

            this.bracketPairs.add(pair);
        }

    }

    public synchronized void highlightBracket() {
        this.highlightBracket(this.tabData.getCodeArea().getCaretPosition());
    }

    public synchronized void clearBracket() {
        this.highlightBracket(-1);
    }

    static class BracketPair {

        private int start;
        private int end;

        public BracketPair(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return "BracketPair{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }

    }

}
