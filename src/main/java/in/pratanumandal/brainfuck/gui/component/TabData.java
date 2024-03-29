package in.pratanumandal.brainfuck.gui.component;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.Memory;
import in.pratanumandal.brainfuck.engine.debugger.Debugger;
import in.pratanumandal.brainfuck.engine.processor.interpreter.Interpreter;
import in.pratanumandal.brainfuck.gui.component.terminal.Terminal;
import in.pratanumandal.brainfuck.gui.highlight.BracketHighlighter;
import in.pratanumandal.brainfuck.gui.highlight.Highlighter;
import in.pratanumandal.brainfuck.os.windows.WindowsUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TabData {

    private final Tab tab;
    private final SplitPane splitPane;
    private final CodePad codePad;

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
    private Terminal debugTerminal;

    private Interpreter interpreter;
    private Terminal interpretTerminal;

    private BracketHighlighter bracketHighlighter;

    private String filePath;

    private boolean modified;
    private boolean untitled;
    private boolean largeFile;

    private int currentMemoryPointer;

    private double dividerPosition;

    private ScheduledFuture<?> saveFuture;

    private Thread fileSystemChangesThread;
    private boolean pauseAutoSave;

    // untitled tab index
    private static int untitledTabIndex = 1;

    public TabData(Tab tab, SplitPane splitPane, CodePad codePad, String filePath) throws IOException {
        this.tab = tab;
        this.splitPane = splitPane;
        this.codePad = codePad;
        this.filePath = filePath;
        this.modified = false;
        this.dividerPosition = 0.5;
        this.memory = FXCollections.observableArrayList();

        if (this.filePath == null) {
            // set tab text
            this.tab.setText("Untitled " + untitledTabIndex++);

            // set untitled
            this.setUntitled(true);
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
            this.codePad.replaceText(fileText);

            // forget undo history
            codePad.getUndoManager().forgetHistory();

            // start highlighter
            Highlighter.refreshHighlighting(this);

            // register file change listener
            this.registerChangeListener();
        }

        codePad.textProperty().addListener((observableValue, oldVal, newVal) -> {
            this.setModified(true);
        });

        codePad.setOnKeyPressed(event -> this.pauseAutoSave = true);
        codePad.setOnKeyReleased(event -> this.pauseAutoSave = false);

        this.registerAutoSave();
    }

    public Tab getTab() {
        return tab;
    }

    public SplitPane getSplitPane() {
        return splitPane;
    }

    public CodePad getCodePad() {
        return codePad;
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

    public Terminal getDebugTerminal() {
        return debugTerminal;
    }

    public void setDebugTerminal(Terminal debugTerminal) {
        this.debugTerminal = debugTerminal;
    }

    public Terminal getInterpretTerminal() {
        return interpretTerminal;
    }

    public void setInterpretTerminal(Terminal interpretTerminal) {
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
                memory.add(i, new Memory(i + 1));
            }
        }

        // reset memory pointer
        memory.get(currentMemoryPointer).setCurrent(false);
        this.currentMemoryPointer = 0;
        memory.get(currentMemoryPointer).setCurrent(true);

        // create context menu
        ContextMenu menu = new ContextMenu();

        MenuItem item = new MenuItem("Go to current memory location");
        menu.getItems().add(item);

        item.setOnAction(AE -> Platform.runLater(() -> {
            tableViewExtra.scrollToIndex(currentMemoryPointer);
            tableView.getSelectionModel().select(currentMemoryPointer);
        }));

        tableView.setContextMenu(menu);
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

    public void setBracketHighlighter(BracketHighlighter bracketHighlighter) {
        this.bracketHighlighter = bracketHighlighter;

        // highlight brackets
        bracketHighlighter.initializeBrackets(this.getFileText());
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

        // set tab tooltip
        Tooltip tooltip = new Tooltip(file.getAbsolutePath());
        tooltip.setShowDelay(Duration.millis(300));
        tab.setTooltip(tooltip);

        // re-register file change listener
        this.unregisterChangeListener();
        this.registerChangeListener();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;

        if (this.modified) {
            this.tab.getStyleClass().add("modified");
        }
        else {
            this.tab.getStyleClass().removeAll("modified");
        }
    }

    public void setUntitled(boolean untitled) {
        this.untitled = untitled;

        if (this.untitled) {
            this.tab.getStyleClass().add("untitled");
        }
        else {
            this.tab.getStyleClass().removeAll("untitled");
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
                    codePad.clearStyle(0, getFileText().length());

                    tab.getTabPane().getSelectionModel().select(tab);

                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle(Constants.APPLICATION_NAME);
                    alert.setContentText("Highlighting has been disabled for this tab due to heavy load. This is usually done for files that are very complex to process.\n\n");

                    WindowsUtils.setStageStyle((Stage) alert.getDialogPane().getScene().getWindow());

                    alert.initOwner(tab.getTabPane().getScene().getWindow());
                    alert.showAndWait();
                });
            }
        }
    }

    public void setCurrentMemoryPointer(int memoryPointer) {
        memory.get(currentMemoryPointer).setCurrent(false);
        this.currentMemoryPointer = memoryPointer;
        memory.get(currentMemoryPointer).setCurrent(true);

        Platform.runLater(() -> {
            tableViewExtra.scrollToIndex(memoryPointer);
            tableView.getSelectionModel().select(memoryPointer);
        });
    }

    public double getDividerPosition() {
        return dividerPosition;
    }

    public void setDividerPosition(double dividerPosition) {
        this.dividerPosition = dividerPosition;
    }

    public String getFileText() {
        return codePad.getText();
    }

    private void registerAutoSave() {
        this.saveFuture = Constants.EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            if (this.filePath != null && modified && Configuration.getAutoSave() && !pauseAutoSave) {
                String filePath = this.filePath;
                String fileText = this.getFileText();

                boolean success = saveFile(filePath, fileText);

                if (success) {
                    Platform.runLater(() -> this.setModified(false));
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
            pauseAutoSave = false;
            return true;
        } catch (IOException e) {
            // fail silently here
            e.printStackTrace();
            return false;
        }
    }

    private void registerChangeListener() {
        this.fileSystemChangesThread = new Thread(() -> {
            Path path = Path.of(filePath);
            Path parent = path.getParent();

            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                parent.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                WatchKey watchKey;
                while ((watchKey = watchService.take()) != null) {
                    Thread.sleep(50);
                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                        final Path changed = (Path) event.context();
                        if (changed.equals(path.getFileName())) {
                            if (path.toFile().exists()) {
                                applyFileSystemChanges(FileSystemState.MODIFIED);
                            }
                            else {
                                applyFileSystemChanges(FileSystemState.DELETED);
                            }
                        }
                    }
                    watchKey.reset();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e) { }
        });
        this.fileSystemChangesThread.start();
    }

    public void unregisterChangeListener() {
        if (this.fileSystemChangesThread != null && this.fileSystemChangesThread.isAlive()) {
            this.fileSystemChangesThread.interrupt();
            try {
                this.fileSystemChangesThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyFileSystemChanges(FileSystemState fileSystemState) {
        if (filePath != null) {
            Path path = Path.of(filePath);

            if (fileSystemState == FileSystemState.MODIFIED) {
                if (this.pauseAutoSave) {
                    this.pauseAutoSave = false;
                    Platform.runLater(() -> this.setModified(false));
                }

                try {
                    String fileText = Files.readString(path);

                    if (!fileText.equals(this.getFileText())) {
                        if (this.codePad.isEditable()) {
                            Platform.runLater(() -> {
                                this.codePad.replaceText(fileText);
                                this.setModified(false);
                                Utils.addNotificationWithDelay(path.getFileName() + " was modified outside of Brainfuck Studio", 5000);
                            });
                        }
                        else {
                            AtomicReference<ChangeListener<Boolean>> changeListener = new AtomicReference<>();
                            changeListener.set((obs, oldVal, newVal) -> {
                                if (newVal) {
                                    Platform.runLater(() -> {
                                        this.codePad.replaceText(fileText);
                                        this.setModified(false);
                                        Utils.addNotificationWithDelay(path.getFileName() + " was modified outside of Brainfuck Studio", 5000);
                                    });
                                }
                                this.codePad.editableProperty().removeListener(changeListener.get());
                            });
                            this.codePad.editableProperty().addListener(changeListener.get());
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (fileSystemState == FileSystemState.DELETED) {
                this.pauseAutoSave = true;
                Platform.runLater(() -> {
                    this.setModified(true);
                    Utils.addNotificationWithDelay(path.getFileName() + " was deleted from the file system", 5000);
                });
            }
        }
    }

    public enum FileSystemState {
        MODIFIED, DELETED
    }

}
