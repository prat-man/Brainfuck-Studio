package in.pratanumandal.brainfuck.gui.component;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.component.TabData;
import in.pratanumandal.brainfuck.gui.component.TextInsertionListener;
import in.pratanumandal.brainfuck.gui.highlight.Highlighter;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.Selection;
import org.fxmisc.richtext.model.StyledDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodePad extends CodeArea {

    private TabData tabData;
    private List<TextInsertionListener> insertionListeners = new ArrayList<>();
    private AtomicInteger lastBracketDelete = new AtomicInteger(-1);

    public CodePad() {
        super();
    }

    public void addTextInsertionListener(TextInsertionListener listener) {
        insertionListeners.add(listener);
    }

    public void removeTextInsertionListener(TextInsertionListener listener) {
        insertionListeners.remove(listener);
    }

    public TabData getTabData() {
        return tabData;
    }

    public void setTabData(TabData tabData) {
        this.tabData = tabData;
    }

    public Integer getLastBracketDelete() {
        return this.lastBracketDelete.get();
    }

    @Override
    public void replace(int start, int end, StyledDocument<Collection<String>, String, Collection<String>> replacement) {
        // get replacement text
        String text = replacement.getText();

        // notify all listeners
        for (TextInsertionListener listener : insertionListeners) {
            listener.codeInserted(start, end, text);
        }

        // set last change
        if (text.isEmpty() && getText(start, end).equals("[") && getSelectedText().isEmpty()) this.lastBracketDelete.set(start);
        else this.lastBracketDelete.set(-1);

        // call super
        super.replace(start, end, replacement);

        // recompute highlighting
        if (start != end && tabData != null) {
            Highlighter.refreshHighlighting(tabData);
        }
    }

    @Override
    public void setContextMenu(ContextMenu menu) {
        MenuItem goToLine = new MenuItem("Go to Line");
        goToLine.setOnAction(event -> Utils.goToLine(this.tabData));
        menu.getItems().add(goToLine);

        super.setContextMenu(menu);

        this.registerShortcuts();
    }

    private void registerShortcuts() {
        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.L) {
                event.consume();
                Utils.goToLine(tabData);
            }
            else if (event.getCode() == KeyCode.TAB) {
                event.consume();

                int tabCount = 4;
                String tabPattern = "(\t|\s{1," + tabCount + "})";
                String tabString = Configuration.getUseSpaces() ? " ".repeat(tabCount) : "\t";

                IndexRange selection = this.getCurrentSelection();
                IndexRange realSelection = this.getSelection();
                String selectionText = this.getText(selection);
                String realSelectionText = this.getText(realSelection);
                int realCaretPosition = this.getCaretPosition();

                if (this.getSelection().getLength() > 0) {
                    if (event.isShiftDown()) {
                        String text = selectionText;
                        text = text.replaceAll("\n" + tabPattern, "\n");
                        text = text.replaceAll("^" + tabPattern, "");
                        this.replaceText(selection, text);

                        // selection
                        int start = realSelection.getStart();
                        Pattern pattern = Pattern.compile("^" + tabPattern);
                        Matcher matcher = pattern.matcher(selectionText);
                        if (matcher.find()) {
                            String match = matcher.group();
                            start -= match.length();
                        }
                        int end = start + realSelectionText.length() + (text.length() - selectionText.length());
                        if (start < 0) start = 0;
                        this.selectRange(start, end);
                    }
                    else {
                        String text = selectionText;
                        text = text.replaceAll("\n", "\n" + tabString);
                        text = text.replaceAll("^", tabString);
                        this.replaceText(selection, text);

                        // selection
                        int start = realSelection.getStart();
                        int end = start + realSelectionText.length() + (text.length() - selectionText.length());
                        start += tabCount;
                        this.selectRange(start, end);
                    }
                } else {
                    if (event.isShiftDown()) {
                        String text = selectionText;
                        text = text.replaceAll("\n" + tabPattern, "\n");
                        text = text.replaceAll("^" + tabPattern, "");
                        this.replaceText(selection, text);

                        // caret position
                        int position = realCaretPosition + realSelectionText.length() + (text.length() - selectionText.length());
                        this.moveTo(position);
                    }
                    else {
                        this.insertText(this.getCaretPosition(), tabString);
                    }
                }
            }
        });
    }

    private IndexRange getCurrentSelection() {
        Selection<?, ?, ?> selection = this.getCaretSelectionBind();
        int startParagraph = selection.getStartParagraphIndex();
        int startIndex = this.getAbsolutePosition(startParagraph, 0);
        int endParagraph = selection.getEndParagraphIndex();
        int endIndex = this.getAbsolutePosition(endParagraph, this.getParagraphLength(endParagraph));
        return new IndexRange(startIndex, endIndex);
    }

}
