package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.common.Utils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyledDocument;

import java.util.*;
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
            List<PlainTextChange> changes = new ArrayList<>();
            PlainTextChange change = new PlainTextChange(start, null, text);
            changes.add(change);
            Highlighter.computeHighlighting(changes, tabData);
        }
    }

    @Override
    public void setContextMenu(ContextMenu menu) {
        MenuItem goToLine = new MenuItem("Go to line");
        goToLine.setOnAction(event -> Utils.goToLine(this.tabData));
        menu.getItems().add(goToLine);

        super.setContextMenu(menu);

        this.registerShortcuts();
    }

    private void registerShortcuts() {
        this.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.L) {
                Utils.goToLine(tabData);
            }
        });

        this.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.isControlDown() && (event.getCode() == KeyCode.Z || event.getCode() == KeyCode.Y)) {
                Highlighter.refreshHighlighting(tabData);
            }
        });
    }

}
