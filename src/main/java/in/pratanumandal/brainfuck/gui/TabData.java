package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.engine.debugger.Debugger;
import in.pratanumandal.brainfuck.engine.Memory;
import in.pratanumandal.brainfuck.engine.processor.interpreter.Interpreter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
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

    private final Tab tab;
    private final SplitPane splitPane;
    private final CustomCodeArea codeArea;

    private Button debugResumeButton;
    private Button debugPauseButton;
    private Button debugStepButton;
    private ToggleButton debugBreakpointButton;
    private Button debugStopButton;
    private Button debugCloseButton;
    private Slider debugSpeed;

    private Button interpretStopButton;
    private Button interpretCloseButton;

    private VBox debug;
    private ObservableList<Memory> memory;
    private TableView<Memory> tableView;
    private TableViewExtra<Memory> tableViewExtra;

    private Debugger debugger;
    private FXTerminal debugTerminal;

    private Interpreter interpreter;
    private FXTerminal interpretTerminal;

    private final BracketHighlighter bracketHighlighter;

    private String filePath;
    private boolean modified;

    private boolean largeFile;

    private String tabHeader;

    private double dividerPosition;

    private ScheduledFuture<?> saveFuture;

    // untitled tab index
    private static int untitledTabIndex = 1;

    public TabData(Tab tab, SplitPane splitPane, CustomCodeArea codeArea, BracketHighlighter bracketHighlighter, String filePath) throws IOException {
        this.tab = tab;
        this.splitPane = splitPane;
        this.codeArea = codeArea;
        this.bracketHighlighter = bracketHighlighter;
        this.filePath = filePath;
        this.modified = false;
        this.dividerPosition = 0.5;
        this.memory = FXCollections.observableArrayList();

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

            // highlight brackets
            bracketHighlighter.initializeBrackets(fileText);
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

    public Button getDebugResumeButton() {
        return debugResumeButton;
    }

    public void setDebugResumeButton(Button debugResumeButton) {
        this.debugResumeButton = debugResumeButton;
    }

    public Button getDebugPauseButton() {
        return debugPauseButton;
    }

    public void setDebugPauseButton(Button debugPauseButton) {
        this.debugPauseButton = debugPauseButton;
    }

    public Button getDebugStepButton() {
        return debugStepButton;
    }

    public void setDebugStepButton(Button debugStepButton) {
        this.debugStepButton = debugStepButton;
    }

    public ToggleButton getDebugBreakpointButton() {
        return debugBreakpointButton;
    }

    public void setDebugBreakpointButton(ToggleButton debugBreakpointButton) {
        this.debugBreakpointButton = debugBreakpointButton;
    }

    public Button getDebugStopButton() {
        return debugStopButton;
    }

    public void setDebugStopButton(Button debugStopButton) {
        this.debugStopButton = debugStopButton;
    }

    public Button getDebugCloseButton() {
        return debugCloseButton;
    }

    public void setDebugCloseButton(Button debugCloseButton) {
        this.debugCloseButton = debugCloseButton;
    }

    public Slider getDebugSpeed() {
        return debugSpeed;
    }

    public void setDebugSpeed(Slider debugSpeed) {
        this.debugSpeed = debugSpeed;
    }

    public Button getInterpretStopButton() {
        return interpretStopButton;
    }

    public void setInterpretStopButton(Button interpretStopButton) {
        this.interpretStopButton = interpretStopButton;
    }

    public Button getInterpretCloseButton() {
        return interpretCloseButton;
    }

    public void setInterpretCloseButton(Button interpretCloseButton) {
        this.interpretCloseButton = interpretCloseButton;
    }

    public FXTerminal getDebugTerminal() {
        return debugTerminal;
    }

    public void setDebugTerminal(FXTerminal debugTerminal) {
        this.debugTerminal = debugTerminal;
    }

    public FXTerminal getInterpretTerminal() {
        return interpretTerminal;
    }

    public void setInterpretTerminal(FXTerminal interpretTerminal) {
        this.interpretTerminal = interpretTerminal;
    }

    public TableView<Memory> getTableView() {
        return tableView;
    }

    public TableViewExtra<Memory> getTableViewExtra() {
        return tableViewExtra;
    }

    public void initializeDebugger() {
        this.debugger = Debugger.getDebugger(this);

        // reset table view extra rows
        tableViewExtra.resetRows();

        // initialize the memory
        if (memory.isEmpty() || memory.size() != Configuration.getMemorySize()) {
            ObservableList<Memory> memory = this.getMemory();
            memory.clear();
            for (int i = 0; i < Configuration.getMemorySize(); i++) {
                memory.add(i, new Memory(i + 1, 0, (char) 0));
            }
        }
    }

    public Debugger getDebugger() {
        return this.debugger;
    }

    public void initializeInterpreter() {
        this.interpreter = Interpreter.getInterpreter(this);
    }

    public Interpreter getInterpreter() {
        return this.interpreter;
    }

    public BracketHighlighter getBracketHighlighter() {
        return bracketHighlighter;
    }

    public void setTableView(TableView<Memory> tableView) {
        this.tableView = tableView;
        this.tableViewExtra = new TableViewExtra<>(tableView);
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
            if (this.filePath != null && modified && Configuration.getAutoSave()) {
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
