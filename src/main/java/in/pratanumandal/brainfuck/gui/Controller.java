package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.engine.Memory;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.terminal.Terminal;
import in.pratanumandal.brainfuck.gui.terminal.config.TerminalConfig;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {

    private final List<TabData> tabDataList = new ArrayList<>();

    private final ChangeListener charCountListener = (ChangeListener<String>) (observableValue, oldValue, newValue) -> { updateTextStatus(); };
    private final ChangeListener caretPositonListener = (ChangeListener<Integer>) (observableValue, oldValue, newValue) -> { updateCaretStatus(); };

    private TabData currentTab;

    private String fontSize = "16px";

    private Stage stage;

    private boolean regex;

    @FXML private TabPane tabPane;

    @FXML private ChoiceBox<String> fontSizeChooser;

    @FXML private ToggleButton searchButton;
    @FXML private HBox findAndReplace;
    @FXML private TextField findField;
    @FXML private TextField replaceField;

    @FXML private Label caretPosition;
    @FXML private Label caretRow;
    @FXML private Label caretColumn;

    @FXML private Label charCount;
    @FXML private Label lineCount;

    @FXML
    public void initialize() {
        // add a new untitled tab in the beginning
        TabData tabData = addUntitledTab();

        // allow reordering of tabs
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        // change font size based on selection
        fontSizeChooser.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> observableValue, String oldVal, String newVal) -> {
            fontSize = newVal;

            for (TabData td : tabDataList) {
                td.getCodeArea().setStyle("-fx-font-size: " + fontSize);
            }
        });

        // toggle search
        searchButton.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            findAndReplace.setVisible(newVal);
        });

        // bind managed property with visible property find and replace
        findAndReplace.managedProperty().bind(findAndReplace.visibleProperty());
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        // define key events
        final KeyCombination keyComb1 = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb2 = new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb3 = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb4 = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb5 = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);

        // handle key events
        stage.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (keyComb1.match(event)) {
                saveFile();
            }
            else if (keyComb2.match(event)) {
                saveAsFile();
            }
            else if (keyComb3.match(event)) {
                openFile();
            }
            else if (keyComb4.match(event)) {
                addNewFile();
            }
            else if (keyComb5.match(event)) {
                toggleSearch();
            }
        });
    }

    private TabData addUntitledTab() {
        TabData tabData = null;
        try {
            tabData = createTab();
        } catch (IOException e) {
            // should not occur in general
        }

        Tab tab = tabData.getTab();

        ObservableList<Tab> tabs = tabPane.getTabs();
        tabs.add(tabs.size(), tab);
        tabPane.getSelectionModel().select(tab);

        return tabData;
    }

    private TabData createTab() throws IOException {
        return createTab(null);
    }

    private TabData createTab(String filePath) throws IOException {
        // create a new tab
        Tab tab = new Tab();

        // create a horizontal split pane
        SplitPane horizontalSplitPane = new SplitPane();
        horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);

        // create a split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        horizontalSplitPane.getItems().add(splitPane);

        // create a new code area
        CustomCodeArea codeArea = new CustomCodeArea();

        // set context menu for code area
        codeArea.setContextMenu(new DefaultContextMenu());

        // set font size for code area
        codeArea.setStyle("-fx-font-size: " + fontSize);

        // add line numbers to the left of area
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        // enable text wrapping
        codeArea.setWrapText(true);

        // highlight brackets
        BracketHighlighter bracketHighlighter = new BracketHighlighter(codeArea);

        // create new tab data
        TabData tabData = new TabData(tab, splitPane, codeArea, bracketHighlighter, filePath);

        // set tab data of code area
        codeArea.setTabData(tabData);

        // auto complete loops
        codeArea.setOnKeyTyped(keyEvent -> {
            // clear bracket highlighting
            bracketHighlighter.clearBracket();

            // get typed character
            String character = keyEvent.getCharacter();

            // add a ] if [ is typed
            if (character.equals("[")) {
                int position = codeArea.getCaretPosition();
                codeArea.insert(position, "]", "loop");
                codeArea.moveTo(position);
            }
            // remove next ] if ] is typed
            else if (character.equals("]")) {
                int position = codeArea.getCaretPosition();
                if (position != codeArea.getLength()) {
                    String nextChar = codeArea.getText(position, position + 1);
                    if (nextChar.equals("]")) codeArea.deleteText(position, position + 1);
                }
            }
            // remove adjacent ] if [ is removed
            else if (character.equals("\b")) {
                int position = codeArea.getLastBracketDelete();
                if (position != -1) {
                    if (position < codeArea.getLength() && codeArea.getText(position, position + 1).equals("]")) {
                        codeArea.deleteText(position, position + 1);
                    }
                }
            }

            // refresh bracket highlighting
            bracketHighlighter.highlightBracket();
        });

        // recompute the syntax highlighting 500 ms after user stops editing area
        Subscription subscription = codeArea

                // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
                // multi plain changes = save computation by not rerunning the code multiple times
                //   when making multiple changes (e.g. renaming a method at multiple parts in file)
                .multiPlainChanges()

                // do not emit an event until 500 ms have passed since the last emission of previous stream
                //.successionEnds(Duration.ofMillis(100))

                // run the following code block when previous stream emits an event
                //.subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
                .subscribe(changes -> Highlighter.computeHighlighting(changes, tabData));

        // auto-indent: insert previous line's indents on enter
        final Pattern whiteSpace = Pattern.compile("^\\s+");
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED, KE -> {
            if (KE.getCode() == KeyCode.ENTER) {
                int caretPosition = codeArea.getCaretPosition();
                int currentParagraph = codeArea.getCurrentParagraph();
                Matcher m0 = whiteSpace.matcher(codeArea.getParagraph(currentParagraph - 1).getSegments().get(0));
                if (m0.find()) Platform.runLater(() -> codeArea.insertText(caretPosition, m0.group()));
            }
        });

        // create a stack pane to contain the code area
        StackPane codePane = new StackPane();
        codePane.getChildren().add(new VirtualizedScrollPane<>(codeArea));
        splitPane.getItems().add(codePane);

        // create toolbar for debug terminal
        HBox debugTerminalControls = new HBox();
        debugTerminalControls.setPadding(new Insets(9, 5, 10, 5));
        debugTerminalControls.setSpacing(7);
        debugTerminalControls.setAlignment(Pos.CENTER_LEFT);

        // add title to toolbar
        Label label = new Label("Debug Terminal");
        debugTerminalControls.getChildren().add(label);

        // create a debug terminal
        DebugTerminal debugTerminal = new DebugTerminal();

        // set the debug terminal
        tabData.setDebugTerminal(debugTerminal);

        // add debug terminal toolbar and debug terminal to vbox
        VBox debugTerminalToolbar = new VBox();
        debugTerminalToolbar.getChildren().add(debugTerminalControls);
        debugTerminalToolbar.getChildren().add(debugTerminal);

        // set font size for code area
        debugTerminalToolbar.setStyle("-fx-background-color: #585D66;");

        // bind terminal height property
        debugTerminal.prefHeightProperty().bind(debugTerminalToolbar.heightProperty());

        // bind debug terminal visibility
        debugTerminal.setVisible(false);
        debugTerminalToolbar.setVisible(false);
        debugTerminalToolbar.managedProperty().bind(debugTerminal.visibleProperty());
        debugTerminalToolbar.visibleProperty().bind(debugTerminal.visibleProperty());
        debugTerminal.managedProperty().bind(debugTerminal.visibleProperty());
        debugTerminal.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                splitPane.getItems().add(debugTerminalToolbar);
                splitPane.setDividerPositions(tabData.getDividerPosition());
            }
            else {
                tabData.setDividerPosition(tabData.getSplitPane().getDividerPositions()[0]);
                splitPane.getItems().remove(debugTerminalToolbar);
            }
        });

        // create a debug side pane
        VBox debug = new VBox();
        debug.setMinWidth(220);
        debug.setMaxWidth(300);

        debug.setVisible(false);
        debug.managedProperty().bind(debug.visibleProperty());
        debug.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) horizontalSplitPane.getItems().add(debug);
            else horizontalSplitPane.getItems().remove(debug);
        });

        // set the debug side pane
        tabData.setDebug(debug);

        // debug tools
        HBox debugTools = new HBox();
        debugTools.setPadding(new Insets(10, 5, 10, 5));
        debugTools.setSpacing(5);
        debugTools.setAlignment(Pos.CENTER_LEFT);
        debug.getChildren().add(debugTools);

        Button resumeButton = generateDebugButton("run", "Resume");
        resumeButton.setOnAction(event -> tabData.getDebugger().resume());
        tabData.setResumeButton(resumeButton);
        debugTools.getChildren().add(resumeButton);
        resumeButton.setDisable(true);

        Button pauseButton = generateDebugButton("pause", "Pause");
        pauseButton.setOnAction(event -> tabData.getDebugger().pause());
        tabData.setPauseButton(pauseButton);
        debugTools.getChildren().add(pauseButton);

        Button stepButton = generateDebugButton("step", "Step");
        stepButton.setOnAction(event -> tabData.getDebugger().step());
        tabData.setStepButton(stepButton);
        debugTools.getChildren().add(stepButton);

        // add spacer
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        debugTools.getChildren().add(spacer);

        Button stopButton = generateDebugButton("stop", "Stop");
        stopButton.setOnAction(event -> tabData.getDebugger().stop());
        tabData.setStopButton(stopButton);
        debugTools.getChildren().add(stopButton);

        Button closeButton = generateDebugButton("close", "Close");
        closeButton.setOnAction(event -> {
            debug.setVisible(false);
            debugTerminal.setVisible(false);
        });
        tabData.setCloseButton(closeButton);
        debugTools.getChildren().add(closeButton);
        closeButton.setDisable(true);

        // debug speed controls
        HBox debugSpeedControls = new HBox();
        debugSpeedControls.setPadding(new Insets(10, 10, 15, 10));
        debugSpeedControls.setSpacing(10);
        debugSpeedControls.setAlignment(Pos.CENTER);
        debug.getChildren().add(debugSpeedControls);

        Label debugSpeedLabel = new Label("Speed");
        debugSpeedControls.getChildren().add(debugSpeedLabel);

        Slider debugSpeed = new Slider(0, 450, 350);
        debugSpeed.setMajorTickUnit(50);
        debugSpeed.setMinorTickCount(0);
        debugSpeed.setSnapToTicks(true);
        HBox.setHgrow(debugSpeed, Priority.ALWAYS);
        debugSpeedControls.getChildren().add(debugSpeed);

        // set the debug speed control
        tabData.setDebugSpeed(debugSpeed);

        // create a tableview for memory
        TableView<Memory> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // add to debug side pane
        debug.getChildren().add(tableView);

        // set the tableview
        tabData.setTableView(tableView);

        // initialize the tableview columns
        TableColumn<Memory, Integer> column1 = new TableColumn<>("Address");
        column1.setCellValueFactory(new PropertyValueFactory<>("address"));
        String format1 = "%0" + (int) (Math.log10(Constants.MEMORY_SIZE) + 1) + "d";
        column1.setCellFactory(column -> {
            TableCell<Memory, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if(empty) setText(null);
                    else setText(String.format(format1, item));
                }
            };
            return cell;
        });
        column1.setSortable(false);
        tableView.getColumns().add(column1);

        TableColumn<Memory, Integer> column2 = new TableColumn<>("Data");
        column2.setCellValueFactory(new PropertyValueFactory<>("data"));
        String format2 = "%03d";
        column2.setCellFactory(column -> {
            TableCell<Memory, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if(empty) setText(null);
                    else setText(String.format(format2, item));
                }
            };
            return cell;
        });
        column2.setSortable(false);
        tableView.getColumns().add(column2);

        TableColumn<Memory, Character> column3 = new TableColumn<>("Character");
        column3.setCellValueFactory(new PropertyValueFactory<>("character"));
        column3.setSortable(false);
        tableView.getColumns().add(column3);

        // initialize the memory
        ObservableList<Memory> memory = tabData.getMemory();
        for (int i = 0; i < Constants.MEMORY_SIZE; i++) {
            memory.add(i, new Memory(i + 1, 0, (char) 0));
        }
        tableView.setItems(memory);

        // set tab content
        tab.setContent(horizontalSplitPane);

        // add tab data to tab data list
        tabDataList.add(tabData);

        tab.setOnClosed((event) -> {
            tabDataList.remove(tabData);

            if (tabData.getFilePath() != null) {
                if (tabData.getDebugger().isAlive()) {
                    tabData.getDebugger().stop();
                    tabData.getDebug().setVisible(false);
                    tabData.getDebugTerminal().setVisible(false);
                } else {
                    // remove old vbox from split pane if present
                    Terminal oldTerminal = tabData.getTerminal();
                    if (oldTerminal != null) {
                        oldTerminal.onTerminalFxReady(() -> {
                            Process process = oldTerminal.getProcess();
                            if (process.isAlive()) process.destroyForcibly();
                        });
                        if (tabData.getSplitPane().getItems().size() > 1) {
                            tabData.setDividerPosition(tabData.getSplitPane().getDividerPositions()[0]);
                            tabData.getSplitPane().getItems().remove(1);
                            tabData.setTerminal(null);
                        }
                    }
                }
            }

            if (tabDataList.isEmpty()) {
                addUntitledTab();
            }

            // clean up subscription
            subscription.unsubscribe();

            // unregister auto save
            tabData.unregisterAutoSave();
        });

        tab.setOnCloseRequest(event -> {
            if (tabData.isModified()) {
                tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO);
                alert.setTitle(Constants.APPLICATION_NAME);
                alert.setHeaderText("Discard changes?");
                alert.setContentText("Are you sure you want to discard unsaved changes?\n\n");

                DialogPane pane = alert.getDialogPane();
                for (ButtonType t : alert.getButtonTypes()) {
                    ((Button) pane.lookupButton(t)).setDefaultButton(t == ButtonType.NO);
                }

                alert.initOwner(tabPane.getScene().getWindow());
                alert.showAndWait();

                if (alert.getResult() == ButtonType.NO) {
                    event.consume();
                }

                Platform.runLater(() -> tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER));
            }
        });

        tab.selectedProperty().addListener((ObservableValue<? extends Boolean> observableValue, Boolean oldVal, Boolean newVal) -> {
            if (newVal) {
                Platform.runLater(() -> {
                    codeArea.requestFocus();
                });

                if (currentTab != null) {
                    currentTab.getCodeArea().textProperty().removeListener(charCountListener);
                    currentTab.getCodeArea().caretPositionProperty().removeListener(caretPositonListener);
                }

                currentTab = tabData;
                updateTextStatus();
                updateCaretStatus();

                currentTab.getCodeArea().textProperty().addListener(charCountListener);
                currentTab.getCodeArea().caretPositionProperty().addListener(caretPositonListener);
            }
        });

        return tabData;
    }

    private Button generateDebugButton(String imageStr, String tooltipStr) {
        Button button = new Button();
        button.getStyleClass().add("secondary");

        Image stopImage = new Image(getClass().getClassLoader().getResourceAsStream("images/" + imageStr + ".png"));
        ImageView stopImageView = new ImageView(stopImage);
        button.setGraphic(stopImageView);

        Tooltip tooltip = new Tooltip(tooltipStr);
        tooltip.setShowDelay(Duration.millis(300));
        button.setTooltip(tooltip);

        return button;
    }

    @FXML
    public void addNewFile() {
        addUntitledTab();
    }

    @FXML
    public void openFile() {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(Constants.APPLICATION_NAME);

        fileChooser.setInitialDirectory(Constants.BROWSE_DIRECTORY.get());

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Brainfuck files (*.bf)", "*.bf");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(tabPane.getScene().getWindow());

        if (file != null) {
            Constants.BROWSE_DIRECTORY.set(file.getParentFile());

            try {
                String filePath = file.getAbsolutePath();

                for (TabData data : tabDataList) {
                    if (filePath.equals(data.getFilePath())) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(Constants.APPLICATION_NAME);
                        alert.setContentText("The file is already open in Brainfuck IDE!\n\n");

                        alert.initOwner(tabPane.getScene().getWindow());
                        alert.showAndWait();

                        Tab tab = data.getTab();
                        tabPane.getSelectionModel().select(tab);

                        return;
                    }
                }

                TabData tabData = createTab(filePath);

                Tab tab = tabData.getTab();

                ObservableList<Tab> tabs = tabPane.getTabs();
                tabs.add(tabs.size(), tab);
                tabPane.getSelectionModel().select(tab);

                tabData.getCodeArea().moveTo(0);
                tabData.getCodeArea().scrollYToPixel(0);

            } catch (IOException e) {
                e.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Constants.APPLICATION_NAME);
                alert.setContentText("Failed to load file!\n\n");

                alert.initOwner(tabPane.getScene().getWindow());
                alert.showAndWait();
            }
        }
    }

    @FXML
    public void saveFile() {
        TabData tabData = currentTab;

        if (tabData.getFilePath() == null) {
            saveAsFile();
        }
        else {
            String filePath = tabData.getFilePath();
            String fileText = tabData.getFileText();

            boolean success = saveFile(filePath, fileText);

            if (success) {
                tabData.setModified(false);
            }
        }
    }

    @FXML
    public void saveAsFile() {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(Constants.APPLICATION_NAME);

        fileChooser.setInitialDirectory(Constants.BROWSE_DIRECTORY.get());

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Brainfuck files (*.bf)", "*.bf");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());

        if (file != null) {
            Constants.BROWSE_DIRECTORY.set(file.getParentFile());

            TabData tabData = currentTab;

            String filePath = file.getAbsolutePath();
            String fileText = tabData.getFileText();

            for (TabData data : tabDataList) {
                if (data != tabData && filePath.equals(data.getFilePath())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(Constants.APPLICATION_NAME);
                    alert.setContentText("The file is already open in Brainfuck IDE!\n\n");

                    alert.initOwner(tabPane.getScene().getWindow());
                    alert.showAndWait();

                    Tab tab = data.getTab();
                    tabPane.getSelectionModel().select(tab);

                    return;
                }
            }

            boolean success = saveFile(filePath, fileText);

            if (success) {
                tabData.setFilePath(filePath);
                tabData.setModified(false);
            }
        }
    }

    @FXML
    private void toggleRegex() {
        this.regex = !this.regex;
    }

    @FXML
    private void findNext() {
        this.findNext(true);
    }

    private void findNext(boolean showAlert) {
        TabData tabData = currentTab;

        CodeArea codeArea = tabData.getCodeArea();
        int anchor = codeArea.getCaretPosition();

        String text = tabData.getFileText().substring(anchor);
        String search = findField.getText();

        int start = -1;
        int end = -1;

        if (this.regex) {
            Matcher matcher = Pattern.compile(search).matcher(text);
            if (matcher.find()) {
                start = matcher.start();
                end = matcher.end();
            }
        }
        else {
            start = text.indexOf(search);
            end = start + search.length();
        }

        if (start >= 0) {
            // select the text
            codeArea.selectRange(anchor + start, anchor + end);

            // scroll to selection
            //codeArea.scrollXToPixel(0);
            codeArea.requestFollowCaret();
        }
        else {
            codeArea.moveTo(0);

            if (showAlert) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(Constants.APPLICATION_NAME);
                alert.setContentText("Reached end of file, text not found.\n\n");

                alert.initOwner(tabPane.getScene().getWindow());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void findPrevious() {
        this.findPrevious(true);
    }

    private void findPrevious(boolean showAlert) {
        TabData tabData = currentTab;

        CodeArea codeArea = tabData.getCodeArea();

        IndexRange range = codeArea.getSelection();
        int anchor = range.getLength() > 0 ? Math.min(range.getStart(), range.getEnd()) : codeArea.getCaretPosition();

        String text = tabData.getFileText().substring(0, anchor);
        String search = findField.getText();

        int start = -1;
        int end = -1;

        if (this.regex) {
            Matcher matcher = Pattern.compile(search).matcher(text);
            while (matcher.find()) {
                start = matcher.start();
                end = matcher.end();
            }
        }
        else {
            start = text.lastIndexOf(search);
            end = start + search.length();
        }

        if (start >= 0) {
            // select the text
            codeArea.selectRange(start, end);

            // scroll to selection
            //codeArea.scrollXToPixel(0);
            codeArea.requestFollowCaret();
        }
        else {
            codeArea.moveTo(codeArea.getText().length());

            if (showAlert) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(Constants.APPLICATION_NAME);
                alert.setContentText("Reached beginning of file, text not found.\n\n");

                alert.initOwner(tabPane.getScene().getWindow());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void replace() {
        TabData tabData = currentTab;

        CodeArea codeArea = tabData.getCodeArea();

        String search = findField.getText();
        String replace = replaceField.getText();

        IndexRange range = codeArea.getSelection();
        String selectedText = codeArea.getSelectedText();
        if (range.getLength() == 0 || !selectedText.equalsIgnoreCase(search)) {
            findNext(false);

            range = codeArea.getSelection();
            if (range.getLength() == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(Constants.APPLICATION_NAME);
                alert.setContentText("Reached end of file, text not found.\n\n");

                alert.initOwner(tabPane.getScene().getWindow());
                alert.showAndWait();

                return;
            }
        }

        codeArea.replaceText(range, replace);
        findNext(false);
    }

    @FXML
    private void replaceAll() {
        TabData tabData = currentTab;

        CodeArea codeArea = tabData.getCodeArea();

        String search = findField.getText();
        String replace = replaceField.getText();

        int count = 0;

        codeArea.moveTo(0);

        findNext(false);
        IndexRange range = codeArea.getSelection();

        while (range.getLength() > 0) {
            count++;
            codeArea.replaceText(range, replace);

            findNext(false);
            range = codeArea.getSelection();
        }

        if (count == 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(Constants.APPLICATION_NAME);
            alert.setContentText("Reached end of file, text not found.\n\n");

            alert.initOwner(tabPane.getScene().getWindow());
            alert.showAndWait();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Constants.APPLICATION_NAME);
            alert.setContentText(count + " occurrence of the text was replaced.\n\n");

            alert.initOwner(tabPane.getScene().getWindow());
            alert.showAndWait();
        }
    }

    @FXML
    private void toggleSearch() {
        boolean visible = !findAndReplace.isVisible();
        findAndReplace.setVisible(visible);
        searchButton.setSelected(visible);
    }

    @FXML
    private void about() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Constants.APPLICATION_NAME);

        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        alert.getDialogPane().lookupButton(ButtonType.OK).setManaged(false);
        alert.getDialogPane().getScene().getRoot().getStyleClass().add("about-dialog");

        VBox vBox = new VBox();
        vBox.getStyleClass().add("about");
        vBox.setAlignment(Pos.CENTER);

        ImageView imageView1 = new ImageView();
        Image image1 = new Image(getClass().getClassLoader().getResourceAsStream("images/icon-large.png"));
        imageView1.setImage(image1);
        imageView1.setFitWidth(96.0);
        imageView1.setFitHeight(96.0);
        imageView1.getStyleClass().add("icon");
        vBox.getChildren().add(imageView1);

        Label label1 = new Label(Constants.APPLICATION_NAME + " " + Constants.APPLICATION_VERSION);
        label1.getStyleClass().add("title");
        vBox.getChildren().add(label1);

        Hyperlink hyperlink1 = new Hyperlink("https://github.com/prat-man/Brainfuck-IDE");
        hyperlink1.getStyleClass().add("hyperlink");
        hyperlink1.setOnAction(event -> {
            Utils.browseURL("https://github.com/prat-man/Brainfuck-IDE");
        });
        vBox.getChildren().add(hyperlink1);

        Label label2 = new Label("from");
        label2.getStyleClass().add("subheading1");
        vBox.getChildren().add(label2);

        Label label3 = new Label("Pratanu Mandal");
        label3.getStyleClass().add("subheading2");
        vBox.getChildren().add(label3);

        Hyperlink hyperlink2 = new Hyperlink("https://pratanumandal.in/");
        hyperlink2.getStyleClass().add("hyperlink");
        hyperlink2.setOnAction(event -> {
            Utils.browseURL("https://pratanumandal.in/");
        });
        vBox.getChildren().add(hyperlink2);

        Label label4 = new Label("with");
        label4.getStyleClass().add("subheading3");
        vBox.getChildren().add(label4);

        ImageView imageView2 = new ImageView();
        Image image2 = new Image(getClass().getClassLoader().getResourceAsStream("images/heart.png"));
        imageView2.setImage(image2);
        imageView2.setFitWidth(48.0);
        imageView2.setFitHeight(48.0);
        vBox.getChildren().add(imageView2);

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        vBox.getChildren().add(hBox);

        Label label5 = new Label("Interpreter/Compiler: ");
        label5.getStyleClass().add("subheading4");
        hBox.getChildren().add(label5);

        Label label6 = new Label(Constants.INTERPRETER_COMPILER_VERSION);
        label6.getStyleClass().add("subheading5");
        hBox.getChildren().add(label6);

        Hyperlink hyperlink3 = new Hyperlink("https://github.com/prat-man/Brainfuck");
        hyperlink3.getStyleClass().add("hyperlink-sm");
        hyperlink3.setOnAction(event -> {
            Utils.browseURL("https://github.com/prat-man/Brainfuck");
        });
        vBox.getChildren().add(hyperlink3);

        alert.getDialogPane().setContent(vBox);

        alert.initOwner(tabPane.getScene().getWindow());
        alert.showAndWait();
    }

    @FXML
    private void debug() {
        TabData tabData = currentTab;
        saveFile();
        if (tabData.getFilePath() != null) {
            if (tabData.getDebugger().isAlive()) {
                tabData.getDebugger().stop();
                tabData.getDebug().setVisible(false);
                tabData.getDebugTerminal().setVisible(false);
            }
            else {
                // remove old vbox from split pane if present
                Terminal oldTerminal = tabData.getTerminal();
                if (oldTerminal != null) {
                    oldTerminal.onTerminalFxReady(() -> {
                        Process process = oldTerminal.getProcess();
                        if (process.isAlive()) process.destroyForcibly();
                    });
                    if (tabData.getSplitPane().getItems().size() > 1) {
                        tabData.setDividerPosition(tabData.getSplitPane().getDividerPositions()[0]);
                        tabData.getSplitPane().getItems().remove(1);
                        tabData.setTerminal(null);
                    }
                }
            }

            tabData.getDebug().setVisible(true);
            tabData.getDebugTerminal().setVisible(true);
            tabData.getTableView().scrollTo(0);
            tabData.getTableView().getSelectionModel().select(0);
            tabData.getDebugger().start();
        }
    }

    @FXML
    private void interpret() {
        TabData tabData = currentTab;
        saveFile();
        if (tabData.getFilePath() != null) {
            if (tabData.getDebugger().isAlive()) {
                tabData.getDebugger().stop();
            }
            tabData.getDebug().setVisible(false);
            tabData.getDebugTerminal().setVisible(false);

            String filePath = tabData.getFilePath();
            String escapedFilePath = filePath.replace("\\", "\\\\");
            execute(tabData, "Interpret", Constants.getExecutablePath(), escapedFilePath);
        }
    }

    @FXML
    private void build() {
        TabData tabData = currentTab;
        saveFile();
        if (tabData.getFilePath() != null) {
            if (tabData.getDebugger().isAlive()) {
                tabData.getDebugger().stop();
            }
            tabData.getDebug().setVisible(false);
            tabData.getDebugTerminal().setVisible(false);

            String filePath = tabData.getFilePath();
            String escapedFilePath = filePath.replace("\\", "\\\\");
            execute(tabData, "Build", Constants.getExecutablePath(), "-c", escapedFilePath);
        }
    }

    @FXML
    private void buildAndRun() {
        TabData tabData = currentTab;
        saveFile();
        if (tabData.getFilePath() != null) {
            if (tabData.getDebugger().isAlive()) {
                tabData.getDebugger().stop();
            }
            tabData.getDebug().setVisible(false);
            tabData.getDebugTerminal().setVisible(false);

            String filePath = tabData.getFilePath();
            String escapedFilePath = filePath.replace("\\", "\\\\");
            Future<Integer> future = execute(tabData, "Build", Constants.getExecutablePath(), "-c", escapedFilePath);

            Thread thread = new Thread(() -> {
                try {
                    // get return code
                    int returnCode = future.get();

                    Platform.runLater(() -> {
                        // generate output file path
                        String outputFilePath = filePath.substring(0, filePath.length() - 3);
                        if (SystemUtils.IS_OS_WINDOWS) {
                            outputFilePath += ".exe";
                        }

                        // create file object for output file
                        File outputFile = new File(outputFilePath);

                        // check return code and existence of output file
                        if (returnCode == 0 && outputFile.exists()){
                            // try to run executable
                            outputFile.setExecutable(true);
                            String escapedOutputFilePath = outputFilePath.replace("\\", "\\\\");
                            execute(tabData, "Run", escapedOutputFilePath);
                        } else {
                            // show error message
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle(Constants.APPLICATION_NAME);
                            alert.setContentText("Build failed!\n\n");

                            alert.initOwner(tabPane.getScene().getWindow());
                            alert.showAndWait();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
    }

    private Future<Integer> execute(TabData tabData, String subtitle, String ... command) {
        // get the split pane
        SplitPane splitPane = tabData.getSplitPane();

        // create toolbar for terminal
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(5, 5, 6, 5));
        hBox.setSpacing(7);
        hBox.setAlignment(Pos.CENTER_LEFT);

        // add title to toolbar
        String title = (subtitle == null || subtitle.isEmpty()) ? "Terminal" : "Terminal - " + subtitle;
        Label label = new Label(title);
        hBox.getChildren().add(label);

        // add spacer
        Pane pane = new Pane();
        HBox.setHgrow(pane, Priority.ALWAYS);
        hBox.getChildren().add(pane);

        // add stop button
        Button stopButton = new Button();
        Image stopImage = new Image(getClass().getClassLoader().getResourceAsStream("images/stop.png"));
        ImageView stopImageView = new ImageView(stopImage);
        stopButton.setGraphic(stopImageView);
        stopButton.getStyleClass().add("secondary");
        hBox.getChildren().add(stopButton);

        // set stop button tooltip
        Tooltip stopTooltip = new Tooltip("Stop");
        stopTooltip.setShowDelay(Duration.millis(300));
        stopButton.setTooltip(stopTooltip);

        // add close button
        Button closeButton = new Button();
        Image closeImage = new Image(getClass().getClassLoader().getResourceAsStream("images/close.png"));
        ImageView closeImageView = new ImageView(closeImage);
        closeButton.setGraphic(closeImageView);
        closeButton.getStyleClass().add("secondary");
        closeButton.setDisable(true);
        hBox.getChildren().add(closeButton);

        // set close button tooltip
        Tooltip closeTooltip = new Tooltip("Close");
        closeTooltip.setShowDelay(Duration.millis(300));
        closeButton.setTooltip(closeTooltip);

        // create a new terminal
        Terminal terminal = new Terminal(command);

        // set terminal look and feel
        TerminalConfig darkConfig = new TerminalConfig();
        darkConfig.setBackgroundColor(Color.rgb(68,73,82));
        darkConfig.setForegroundColor(Color.rgb(240, 240, 240));
        darkConfig.setCursorColor(Color.rgb(255, 255, 255, 0.5));
        terminal.setTerminalConfig(darkConfig);

        // add terminal toolbar and terminal to vbox
        VBox vBox = new VBox();
        vBox.getChildren().add(hBox);
        vBox.getChildren().add(terminal);

        // set font size for code area
        vBox.setStyle("-fx-background-color: #585D66;");

        // bind terminal height property
        terminal.prefHeightProperty().bind(vBox.heightProperty());

        // remove old vbox from split pane if present
        Terminal oldTerminal = tabData.getTerminal();
        if (oldTerminal != null) {
            oldTerminal.onTerminalFxReady(() -> {
                Process process = oldTerminal.getProcess();
                if (process.isAlive()) process.destroyForcibly();
            });
            if (splitPane.getItems().size() > 1) {
                tabData.setDividerPosition(splitPane.getDividerPositions()[0]);
                splitPane.getItems().remove(1);
                tabData.setTerminal(null);
            }
        }

        // add vbox to split pane
        splitPane.getItems().add(vBox);
        splitPane.setDividerPositions(tabData.getDividerPosition());
        tabData.setTerminal(terminal);

        // add stop button action
        stopButton.setOnAction(actionEvent -> {
            terminal.getProcess().destroyForcibly();
        });

        // add close button action
        closeButton.setOnAction(actionEvent -> {
            tabData.setDividerPosition(splitPane.getDividerPositions()[0]);
            splitPane.getItems().remove(1);
            tabData.setTerminal(null);
        });

        // create a new executor
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // wait for process to finish
        return executor.submit(() -> {
            CountDownLatch latch = new CountDownLatch(1);

            AtomicInteger returnCode = new AtomicInteger();

            // wait for the terminal to finish
            terminal.onTerminalFxReady(() -> {
                try {
                    // start time
                    long startTime = System.nanoTime();

                    // wait for process to terminate
                    returnCode.set(terminal.getProcess().waitFor());

                    // stop time
                    long stopTime = System.nanoTime();

                    // execution duration
                    long duration = stopTime - startTime;
                    String durationStr = nanoToBestFitTimeUnits(duration);

                    // print the execution time
                    terminal.printText("\r\n\r\n");
                    terminal.printText("--------------------------------------------------------------------------------\r\n");
                    terminal.printText("Execution completed in " + durationStr + "\r\n");
                    terminal.printText("Process terminated with status " + returnCode + "\r\n");

                    // change button states
                    stopButton.setDisable(true);
                    closeButton.setDisable(false);

                    // let all threads proceed
                    latch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return returnCode.get();
        });
    }

    /*public static void runAndWait(Runnable action) {
        if (action == null)
            throw new NullPointerException("action");

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        // queue on JavaFX thread and wait for completion
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                doneLatch.countDown();
            }
        });

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }*/

    private boolean saveFile(String filePath, String fileText) {
        try {
            Files.writeString(Path.of(filePath), fileText);

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(Constants.APPLICATION_NAME);
            alert.setContentText("Failed to load file!\n\n");

            alert.initOwner(tabPane.getScene().getWindow());
            alert.showAndWait();

            return false;
        }
    }

    private void updateTextStatus() {
        String currentTabText = currentTab.getFileText();

        int chars = currentTabText.length();
        String charsStr = NumberFormat.getNumberInstance(Locale.US).format(chars);
        charCount.setText(charsStr + " characters");

        int lines = countNewlines(currentTabText);
        String linesStr = NumberFormat.getNumberInstance(Locale.US).format(lines);
        lineCount.setText(linesStr + " lines");
    }

    private void updateCaretStatus() {
        String currentTabText = currentTab.getFileText();
        CodeArea codeArea = currentTab.getCodeArea();

        int pos = codeArea.getCaretPosition();
        int row = countNewlines(currentTabText.substring(0, pos));
        int col = codeArea.getCaretColumn() + 1;

        String posStr = NumberFormat.getNumberInstance(Locale.US).format(pos);
        String rowStr = NumberFormat.getNumberInstance(Locale.US).format(row);
        String colStr = NumberFormat.getNumberInstance(Locale.US).format(col);

        caretPosition.setText("Position: " + posStr);
        caretRow.setText("Line: " + rowStr);
        caretColumn.setText("Column: " + colStr);
    }

    private int countNewlines(String text) {
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(text);
        int lines = 1;
        while (m.find()) {
            lines ++;
        }
        return lines;
    }

    private String nanoToBestFitTimeUnits(long nano) {
        DecimalFormat df = new DecimalFormat("0.00");

        double seconds = (double) nano / 1_000_000_000.0;
        if (seconds < 60) return df.format(seconds) + " seconds";
        else {
            double minutes = seconds / 60;
            if (minutes < 60) return df.format(minutes) + " minutes";
            else {
                double hours = minutes / 60;
                if (hours < 24) return df.format(hours) + " hours";
                else {
                    double days = hours / 24;
                    return df.format(days) + " days";
                }
            }
        }
    }

}
