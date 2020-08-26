package in.pratanumandal.brainfuck;

import in.pratanumandal.brainfuck.terminal.Terminal;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TabData {

    private Tab tab;
    private SplitPane splitPane;
    private CodeArea codeArea;
    private Terminal terminal;

    private String filePath;
    private boolean modified;

    private boolean largeFile;

    private String tabHeader;

    private double dividerPosition;

    // untitled tab index
    private static int untitledTabIndex = 1;

    public TabData(Tab tab, SplitPane splitPane, CodeArea codeArea, String filePath) throws IOException {
        this.tab = tab;
        this.splitPane = splitPane;
        this.codeArea = codeArea;
        this.filePath = filePath;
        this.modified = false;
        this.dividerPosition = 0.5;

        if (this.filePath == null) {
            tab.setText("Untitled " + untitledTabIndex++);
        }
        else {
            File file = new File(this.filePath);

            // set tab text
            this.tab.setText(file.getName());

            // set tab tooltip
            Tooltip tooltip = new Tooltip(file.getAbsolutePath());
            tooltip.setShowDelay(Duration.millis(300));
            tab.setTooltip(tooltip);

            // set text content
            String fileText = Files.readString(file.toPath());
            this.codeArea.replaceText(fileText);

            // forget undo history
            codeArea.getUndoManager().forgetHistory();

            // start highlighter
            List<PlainTextChange> changes = new ArrayList<>();
            PlainTextChange change = new PlainTextChange(0, null, fileText);
            changes.add(change);
            Highlighter.computeHighlighting(changes, this);
        }

        codeArea.textProperty().addListener((observableValue, oldVal, newVal) -> {
            this.setModified(true);
        });

        tabHeader = tab.getText();
    }

    public Tab getTab() {
        return tab;
    }

    public SplitPane getSplitPane() {
        return splitPane;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;

        File file = new File(this.filePath);

        // set tab text
        this.tab.setText(file.getName());
        tabHeader = tab.getText();

        // set tab tooltip
        Tooltip tooltip = new Tooltip(file.getAbsolutePath());
        tooltip.setShowDelay(Duration.millis(300));
        tab.setTooltip(tooltip);
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;

        if (this.modified) {
            tab.setText("\u25CF " + tabHeader);
        } else {
            this.tab.setText(tabHeader);
        }
    }

    public boolean isLargeFile() {
        return largeFile;
    }

    public void setLargeFile(boolean largeFile) {
        if (this.largeFile) return;
        else {
            this.largeFile = largeFile;

            tab.getStyleClass().add("no-highlighter");

            if (this.largeFile) {
                Platform.runLater(() -> {
                    codeArea.clearStyle(0, getFileText().length());

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(Constants.APPLICATION_NAME);
                    alert.setContentText("Highlighting has been disabled for this tab due to heavy load.\nThis is usually done for files that are very complex to process.\n\n");

                    alert.initOwner(tab.getTabPane().getScene().getWindow());
                    alert.showAndWait();
                });
            }
        }
    }

    public double getDividerPosition() {
        return dividerPosition;
    }

    public void setDividerPosition(double dividerPosition) {
        this.dividerPosition = dividerPosition;
    }

    public String getFileText() {
        return codeArea.getText();
    }

}
