package in.pratanumandal.brainfuck.gui.codearea;

import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.component.TabData;
import in.pratanumandal.brainfuck.gui.component.TextInsertionListener;
import in.pratanumandal.brainfuck.gui.highlight.Highlighter;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.StyledDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomCodeArea extends CodeArea {

    private TabData tabData;
    private List<TextInsertionListener> insertionListeners = new ArrayList<>();
    private AtomicInteger lastBracketDelete = new AtomicInteger(-1);

    public CustomCodeArea() {
        super();
    }

    public CustomCodeArea(String text) {
        super(text);
    }

    public CustomCodeArea(EditableStyledDocument<Collection<String>, String, Collection<String>> document) {
        super(document);
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
            else if (event.getCode() == KeyCode.TAB && this.getSelection().getLength() > 0) {
                event.consume();
                if (event.isShiftDown()) {
                    String text = this.getSelectedText();
                    text = text.replaceAll("\n\t", "\n");
                    text = text.replaceAll("^\t", "");

                    int start = this.getSelection().getStart();
                    this.replaceText(this.getSelection(), text);
                    this.selectRange(start, start + text.length());
                }
                else {
                    String text = this.getSelectedText();
                    text = text.replaceAll("\n", "\n\t");
                    text = text.replaceAll("^", "\t");

                    int start = this.getSelection().getStart();
                    this.replaceText(this.getSelection(), text);
                    this.selectRange(start, start + text.length());
                }
            }
        });
    }

}
