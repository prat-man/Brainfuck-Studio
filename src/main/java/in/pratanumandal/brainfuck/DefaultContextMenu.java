package in.pratanumandal.brainfuck;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.undo.UndoManager;

public class DefaultContextMenu extends ContextMenu {

    private MenuItem cut, copy, delete, paste, undo, redo;
    private GenericStyledArea area;
    private IndexRange range;

    public DefaultContextMenu() {
        showingProperty().addListener( (ob,ov,showing) -> checkMenuItems( showing ) );

        cut = new MenuItem("Cut");
        cut.setOnAction(AE -> { hide(); area.cut(); });

        copy = new MenuItem("Copy");
        copy.setOnAction(AE -> { hide(); area.copy(); });

        delete = new MenuItem("Delete");
        delete.setOnAction(AE -> { hide(); area.deleteText( range ); });

        paste = new MenuItem("Paste");
        paste.setOnAction(AE -> { hide(); area.paste(); });

        redo = new MenuItem("Redo");
        redo.setOnAction(AE -> { hide(); area.redo(); });

        undo = new MenuItem("Undo");
        undo.setOnAction(AE -> { hide(); area.undo(); });

        getItems().addAll(copy, cut, delete, paste, redo, undo);
    }

    private void checkMenuItems(boolean showing) {
        if (!showing) return;

        area = (GenericStyledArea) getOwnerNode();
        UndoManager history = area.getUndoManager();
        undo.setDisable(!history.isUndoAvailable());
        redo.setDisable(!history.isRedoAvailable());

        range = area.getSelection();
        boolean noSelection = range.getLength() == 0;
        delete.setDisable(noSelection);
        copy.setDisable(noSelection);
        cut.setDisable(noSelection);
    }

}
