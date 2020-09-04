package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.engine.Debugger;
import in.pratanumandal.brainfuck.engine.Memory;
import in.pratanumandal.brainfuck.gui.DebugTerminal;
import in.pratanumandal.brainfuck.gui.Highlighter;
import in.pratanumandal.brainfuck.gui.terminal.Terminal;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TabData {

    private Tab tab;
    private SplitPane splitPane;
    private CustomCodeArea codeArea;
    private Terminal terminal;

    private Button resumeButton;
    private Button pauseButton;
    private Button stepButton;
    private Button stopButton;
    private Button closeButton;
    private Slider debugSpeed;

    private ObservableList<Memory> memory;
    private VBox debug;
    private TableView<Memory> tableView;

    private Debugger debugger;
    private DebugTerminal debugTerminal;

    private BracketHighlighter bracketHighlighter;

    private String filePath;
    private boolean modified;

    private boolean largeFile;

    private String tabHeader;

    private double dividerPosition;

    private ScheduledFuture<?> saveFuture;

    // untitled tab index
    private static int untitledTabIndex = 1;

    public TabData(Tab tab, SplitPane splitPane, CustomCodeArea codeArea, String filePath) throws IOException {
        this.tab = tab;
        this.splitPane = splitPane;
        this.codeArea = codeArea;
        this.filePath = filePath;
        this.modified = false;
        this.dividerPosition = 0.5;
        this.memory = FXCollections.observableArrayList();

        this.debugger = new Debugger(this);

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

        registerAutoSave();
    }

    public Tab getTab() {
        return tab;
    }

    public SplitPane getSplitPane() {
        return splitPane;
    }

    public CustomCodeArea getCodeArea() {
        return codeArea;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public Button getResumeButton() {
        return resumeButton;
    }

    public void setResumeButton(Button resumeButton) {
        this.resumeButton = resumeButton;
    }

    public Button getPauseButton() {
        return pauseButton;
    }

    public void setPauseButton(Button pauseButton) {
        this.pauseButton = pauseButton;
    }

    public Button getStepButton() {
        return stepButton;
    }

    public void setStepButton(Button stepButton) {
        this.stepButton = stepButton;
    }

    public Button getStopButton() {
        return stopButton;
    }

    public void setStopButton(Button stopButton) {
        this.stopButton = stopButton;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public void setCloseButton(Button closeButton) {
        this.closeButton = closeButton;
    }

    public Slider getDebugSpeed() {
        return debugSpeed;
    }

    public void setDebugSpeed(Slider debugSpeed) {
        this.debugSpeed = debugSpeed;
    }

    public DebugTerminal getDebugTerminal() {
        return debugTerminal;
    }

    public void setDebugTerminal(DebugTerminal debugTerminal) {
        this.debugTerminal = debugTerminal;
    }

    public TableView<Memory> getTableView() {
        return tableView;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public BracketHighlighter getBracketHighlighter() {
        return bracketHighlighter;
    }

    public void setBracketHighlighter(BracketHighlighter bracketHighlighter) {
        this.bracketHighlighter = bracketHighlighter;
    }

    public void setTableView(TableView<Memory> tableView) {
        this.tableView = tableView;
    }

    public VBox getDebug() {
        return debug;
    }

    public void setDebug(VBox debug) {
        this.debug = debug;
    }

    public ObservableList<Memory> getMemory() {
        return memory;
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

    private void registerAutoSave() {
        this.saveFuture = Constants.EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            if (this.filePath != null) {
                String filePath = this.filePath;
                String fileText = this.getFileText();

                boolean success = saveFile(filePath, fileText);

                if (success) {
                    Platform.runLater(() -> setModified(false));
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void unregisterAutoSave() {
        if (this.saveFuture != null) {
            this.saveFuture.cancel(false);
        }
    }

    private boolean saveFile(String filePath, String fileText) {
        try {
            Files.writeString(Path.of(filePath), fileText);
            return true;
        } catch (IOException e) {
            // fail silently here
            e.printStackTrace();
            return false;
        }
    }

}
