package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.engine.processor.translator.CTranslator;
import in.pratanumandal.brainfuck.engine.processor.translator.JavaTranslator;
import in.pratanumandal.brainfuck.engine.Memory;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.processor.translator.JavaTranslatorFast;
import in.pratanumandal.brainfuck.engine.processor.translator.PythonTranslator;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Controller {

    private final List<TabData> tabDataList = new ArrayList<>();

    private final ChangeListener charCountListener = (ChangeListener<String>) (observableValue, oldValue, newValue) -> { updateTextStatus(); };
    private final ChangeListener caretPositonListener = (ChangeListener<Integer>) (observableValue, oldValue, newValue) -> { updateCaretStatus(); };

    private TabData currentTab;

    private String fontSize;

    private Stage stage;

    private boolean regex;
    private boolean caseSensitive;

    private int wrapSearch;

    private final Object processLock = new Object();

    @FXML private TabPane tabPane;

    @FXML private ComboBox<String> fontSizeChooser;

    @FXML private ToggleButton searchButton;
    @FXML private HBox findAndReplace;
    @FXML private TextField findField;
    @FXML private TextField replaceField;

    @FXML private Label caretPosition;
    @FXML private Label caretRow;
    @FXML private Label caretColumn;

    @FXML private Label charCount;
    @FXML private Label lineCount;

    @FXML private VBox notificationPane;

    @FXML
    public void initialize() {
        // initialize notification manager
        Utils.initializeNotificationManager(notificationPane);

        // show tips at startup
        Utils.showTips();

        // show basic info for first run
        if (Configuration.isFirstRun()) {
            try {
                Path path = Files.writeString(Path.of(Constants.WELCOME_FILE), Constants.WELCOME_TEXT);

                TabData tabData = createTab(path.toFile().getAbsolutePath());

                Tab tab = tabData.getTab();

                ObservableList<Tab> tabs = tabPane.getTabs();
                tabs.add(tabs.size(), tab);
                tabPane.getSelectionModel().select(tab);

                tabData.getCodeArea().moveTo(0);
                tabData.getCodeArea().scrollYToPixel(0);
            }
            catch (IOException e) {
                e.printStackTrace();
                addUntitledTab();
            }
        }
        // add a new untitled tab in the beginning
        else {
            addUntitledTab();
        }

        // allow reordering of tabs
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        // change font size based on selection
        fontSizeChooser.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> observableValue, String oldVal, String newVal) -> {
            fontSize = newVal;

            Configuration.setFontSize(Integer.valueOf(newVal.substring(0, newVal.length() - 2)));
            try {
                Configuration.flush();
            } catch (ConfigurationException | IOException e) {
                // DO NOTHING HERE
                e.printStackTrace();
            }

            for (TabData td : tabDataList) {
                td.getCodeArea().setStyle("-fx-font-size: " + fontSize);
            }
        });

        // select current font
        fontSize = Configuration.getFontSize() + "px";
        fontSizeChooser.getSelectionModel().select(fontSize);
        for (TabData td : tabDataList) {
            td.getCodeArea().setStyle("-fx-font-size: " + fontSize);
        }

        // toggle search
        searchButton.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            findAndReplace.setVisible(newVal);
        });

        // bind managed property with visible property find and replace
        findAndReplace.managedProperty().bind(findAndReplace.visibleProperty());

        // automatically put find text
        findAndReplace.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                String selected = currentTab.getCodeArea().getSelectedText();
                if (!selected.isEmpty()) {
                    findField.setText(selected);
                }
                findField.requestFocus();
                findField.selectAll();
                AnchorPane.setBottomAnchor(notificationPane, 92.0);
            }
            else {
                AnchorPane.setBottomAnchor(notificationPane, 50.0);
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        // define key events
        final KeyCombination keyComb1 = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb2 = new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb3 = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb4 = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb5 = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb6 = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb7 = new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyComb8 = new KeyCodeCombination(KeyCode.F3);
        final KeyCombination keyComb9 = new KeyCodeCombination(KeyCode.F3, KeyCombination.SHIFT_DOWN);

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
            else if (keyComb5.match(event) || keyComb6.match(event) || keyComb7.match(event)) {
                toggleSearch();
            }
            else if (keyComb8.match(event)) {
                findNext();
            }
            else if (keyComb9.match(event)) {
                findPrevious();
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
        codeArea.setWrapText(Configuration.getWrapText());

        // create new tab data
        TabData tabData = new TabData(tab, splitPane, codeArea, filePath);

        // highlight brackets
        BracketHighlighter bracketHighlighter = new BracketHighlighter(tabData);
        tabData.setBracketHighlighter(bracketHighlighter);

        // set tab data of code area
        codeArea.setTabData(tabData);

        // auto complete loops
        codeArea.setOnKeyTyped(keyEvent -> {
            if (Configuration.getAutoComplete()) {
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
            }
        });

        // recompute the syntax highlighting 500 ms after user stops editing area
        Subscription subscription = codeArea

                // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
                // multi plain changes = save computation by not rerunning the code multiple times
                //   when making multiple changes (e.g. renaming a method at multiple parts in file)
                .multiPlainChanges()

                // do not emit an event until 100 ms have passed since the last emission of previous stream
                .successionEnds(java.time.Duration.ofMillis(100))

                // run the following code block when previous stream emits an event
                //.subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
                .subscribe(changes -> Highlighter.refreshHighlighting(tabData));

        // auto-indent: insert previous line's indents on enter
        final Pattern whiteSpace = Pattern.compile("^\\s+");
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED, KE -> {
            if (KE.getCode() == KeyCode.ENTER) {
                Matcher matcher = whiteSpace.matcher(codeArea.getParagraph(codeArea.getCurrentParagraph() - 1).getSegments().get(0));
                if (matcher.find()) Platform.runLater(() -> codeArea.insertText(codeArea.getCaretPosition(), matcher.group()));
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
        Label debugLabel = new Label("Debug Terminal");
        debugTerminalControls.getChildren().add(debugLabel);

        // create a debug terminal
        FXTerminal debugTerminal = new FXTerminal();

        // set the debug terminal
        tabData.setDebugTerminal(debugTerminal);

        // add debug terminal toolbar and debug terminal to vbox
        VBox debugTerminalToolbar = new VBox();
        debugTerminalToolbar.getChildren().add(debugTerminalControls);
        debugTerminalToolbar.getChildren().add(debugTerminal);

        // set style of interpreter toolbar
        debugTerminalToolbar.getStyleClass().add("terminal-toolbar");

        // bind debug terminal height property
        debugTerminal.prefHeightProperty().bind(debugTerminalToolbar.heightProperty());

        // bind debug terminal visibility
        debugTerminal.setVisible(false);
        debugTerminalToolbar.setVisible(false);
        debugTerminalToolbar.managedProperty().bind(debugTerminal.visibleProperty());
        debugTerminalToolbar.visibleProperty().bind(debugTerminal.visibleProperty());
        debugTerminal.managedProperty().bind(debugTerminal.visibleProperty());
        debugTerminal.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> splitPane.getItems().add(debugTerminalToolbar));
                splitPane.setDividerPositions(tabData.getDividerPosition());
            }
            else {
                tabData.setDividerPosition(tabData.getSplitPane().getDividerPositions()[0]);
                Platform.runLater(() -> splitPane.getItems().remove(debugTerminalToolbar));
            }
        });

        // create a debug side pane
        VBox debug = new VBox();
        debug.setMinWidth(235);
        debug.setMaxWidth(300);

        debug.setVisible(false);
        debug.managedProperty().bind(debug.visibleProperty());
        debug.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) Platform.runLater(() -> horizontalSplitPane.getItems().add(debug));
            else Platform.runLater(() -> horizontalSplitPane.getItems().remove(debug));
        });

        // set the debug side pane
        tabData.setDebug(debug);

        // debug tools
        HBox debugTools = new HBox();
        debugTools.setPadding(new Insets(10, 5, 10, 5));
        debugTools.setSpacing(5);
        debugTools.setAlignment(Pos.CENTER_LEFT);
        debug.getChildren().add(debugTools);

        Button debugResumeButton = generateDebugButton("run", "Resume");
        debugResumeButton.setOnAction(event -> tabData.getDebugger().resume());
        tabData.setDebugResumeButton(debugResumeButton);
        debugTools.getChildren().add(debugResumeButton);
        debugResumeButton.setDisable(true);

        Button debugPauseButton = generateDebugButton("pause", "Pause");
        debugPauseButton.setOnAction(event -> tabData.getDebugger().pause());
        tabData.setDebugPauseButton(debugPauseButton);
        debugTools.getChildren().add(debugPauseButton);
        debugPauseButton.setDisable(true);

        Button debugStepButton = generateDebugButton("step", "Step");
        debugStepButton.setOnAction(event -> tabData.getDebugger().step());
        tabData.setDebugStepButton(debugStepButton);
        debugTools.getChildren().add(debugStepButton);
        debugStepButton.setDisable(true);

        ToggleButton debugBreakpointButton = generateDebugToggleButton("breakpoint",
                "breakpoint-disabled",
                "Toggle Breakpoints",
                "Breakpoints enabled for this tab",
                "Breakpoints disabled for this tab",
                true);
        debugBreakpointButton.setOnAction(event -> {
            if (debugBreakpointButton.isSelected()) {
                codeArea.getStyleClass().remove("disable-breakpoint");
            }
            else {
                codeArea.getStyleClass().add("disable-breakpoint");
            }
        });
        tabData.setDebugBreakpointButton(debugBreakpointButton);
        debugTools.getChildren().add(debugBreakpointButton);

        // add spacer
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        debugTools.getChildren().add(spacer);

        Button debugStopButton = generateDebugButton("stop", "Stop");
        debugStopButton.setOnAction(event -> tabData.getDebugger().stop());
        tabData.setDebugStopButton(debugStopButton);
        debugTools.getChildren().add(debugStopButton);
        debugStopButton.setDisable(true);

        Button debugCloseButton = generateDebugButton("close", "Close");
        debugCloseButton.setOnAction(event -> {
            debug.setVisible(false);
            debugTerminal.setVisible(false);
        });
        tabData.setDebugCloseButton(debugCloseButton);
        debugTools.getChildren().add(debugCloseButton);
        debugCloseButton.setDisable(true);
        debugCloseButton.setDisable(false);

        // debug speed controls
        HBox debugSpeedControls = new HBox();
        debugSpeedControls.setPadding(new Insets(10, 10, 15, 10));
        debugSpeedControls.setSpacing(10);
        debugSpeedControls.setAlignment(Pos.CENTER);
        debug.getChildren().add(debugSpeedControls);

        Label debugSpeedLabel = new Label("Speed");
        debugSpeedControls.getChildren().add(debugSpeedLabel);

        Slider debugSpeed = new Slider(0, 475, 350);
        debugSpeed.setMajorTickUnit(25);
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
        column1.setCellFactory(column -> {
            TableCell<Memory, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    String format1 = "%0" + (int) (Math.log10(tableView.getItems().size()) + 1) + "d";
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

        // set the items from the observable memory list
        tableView.setItems(tabData.getMemory());

        // create toolbar for interpreter terminal
        HBox interpreterTerminalControls = new HBox();
        interpreterTerminalControls.setPadding(new Insets(5, 5, 6, 5));
        interpreterTerminalControls.setSpacing(7);
        interpreterTerminalControls.setAlignment(Pos.CENTER_LEFT);

        // add title to toolbar
        Label interpreterLabel = new Label("Interpreter Terminal");
        interpreterTerminalControls.getChildren().add(interpreterLabel);

        // add spacer
        Pane spacerPane = new Pane();
        HBox.setHgrow(spacerPane, Priority.ALWAYS);
        interpreterTerminalControls.getChildren().add(spacerPane);

        // add stop button
        Button interpreterStopButton = new Button();
        Image stopImage = new Image(getClass().getClassLoader().getResourceAsStream("images/stop.png"));
        ImageView stopImageView = new ImageView(stopImage);
        interpreterStopButton.setGraphic(stopImageView);
        interpreterStopButton.getStyleClass().add("secondary");
        interpreterTerminalControls.getChildren().add(interpreterStopButton);
        interpreterStopButton.setDisable(true);

        // set stop button tooltip
        Tooltip stopTooltip = new Tooltip("Stop");
        stopTooltip.setShowDelay(Duration.millis(300));
        interpreterStopButton.setTooltip(stopTooltip);

        // set interpreter stop button
        tabData.setInterpretStopButton(interpreterStopButton);

        // add close button
        Button interpreterCloseButton = new Button();
        Image closeImage = new Image(getClass().getClassLoader().getResourceAsStream("images/close.png"));
        ImageView closeImageView = new ImageView(closeImage);
        interpreterCloseButton.setGraphic(closeImageView);
        interpreterCloseButton.getStyleClass().add("secondary");
        interpreterTerminalControls.getChildren().add(interpreterCloseButton);
        interpreterCloseButton.setDisable(false);

        // set interpreter close button
        tabData.setInterpretCloseButton(interpreterCloseButton);

        // set close button tooltip
        Tooltip closeTooltip = new Tooltip("Close");
        closeTooltip.setShowDelay(Duration.millis(300));
        interpreterCloseButton.setTooltip(closeTooltip);

        // create a interpreter terminal
        FXTerminal interpreterTerminal = new FXTerminal();

        // set the interpreter terminal
        tabData.setInterpretTerminal(interpreterTerminal);

        // add interpreter terminal toolbar and interpreter terminal to vbox
        VBox interpreterTerminalToolbar = new VBox();
        interpreterTerminalToolbar.getChildren().add(interpreterTerminalControls);
        interpreterTerminalToolbar.getChildren().add(interpreterTerminal);

        // set style of interpreter toolbar
        interpreterTerminalToolbar.getStyleClass().add("terminal-toolbar");

        // bind interpreter terminal height property
        interpreterTerminal.prefHeightProperty().bind(interpreterTerminalToolbar.heightProperty());

        // bind interpreter terminal visibility
        interpreterTerminal.setVisible(false);
        interpreterTerminalToolbar.setVisible(false);
        interpreterTerminalToolbar.managedProperty().bind(interpreterTerminal.visibleProperty());
        interpreterTerminalToolbar.visibleProperty().bind(interpreterTerminal.visibleProperty());
        interpreterTerminal.managedProperty().bind(interpreterTerminal.visibleProperty());
        interpreterTerminal.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> splitPane.getItems().add(interpreterTerminalToolbar));
                splitPane.setDividerPositions(tabData.getDividerPosition());
            }
            else {
                tabData.setDividerPosition(tabData.getSplitPane().getDividerPositions()[0]);
                Platform.runLater(() -> splitPane.getItems().remove(interpreterTerminalToolbar));
            }
        });

        // add stop button action
        interpreterStopButton.setOnAction(actionEvent -> tabData.getInterpreter().stop());

        // add close button action
        interpreterCloseButton.setOnAction(actionEvent -> interpreterTerminal.setVisible(false));

        // set tab content
        tab.setContent(horizontalSplitPane);

        // add tab data to tab data list
        tabDataList.add(tabData);

        tab.setOnClosed((event) -> {
            tabDataList.remove(tabData);

            if (tabData.getFilePath() != null) {
                if (tabData.getDebugger() != null && tabData.getDebugger().isAlive()) {
                    tabData.getDebugger().stop();
                    tabData.getDebug().setVisible(false);
                    tabData.getDebugTerminal().setVisible(false);
                } else if (tabData.getInterpreter() != null && tabData.getInterpreter().isAlive()) {
                    tabData.getInterpreter().stop();
                    tabData.getInterpretTerminal().setVisible(false);
                }
            }

            if (tabDataList.isEmpty()) {
                addUntitledTab();
            }

            // clean up subscription
            subscription.unsubscribe();

            // unregister auto save
            tabData.unregisterAutoSave();

            // unregister change listener
            tabData.unregisterChangeListener();

            tabData.getDebugTerminal().destroy();
            tabData.getInterpretTerminal().destroy();
        });

        tab.setOnCloseRequest(event -> {
            if (tabData.isModified()) {
                tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO);
                alert.setTitle(Constants.APPLICATION_NAME);
                alert.setHeaderText("Discard changes?");
                alert.setContentText("Are you sure you want to discard unsaved changes?\n\n");

                Utils.setDefaultButton(alert, ButtonType.NO);

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

        Image image = new Image(getClass().getClassLoader().getResourceAsStream("images/" + imageStr + ".png"));
        ImageView imageView = new ImageView(image);
        button.setGraphic(imageView);

        Tooltip tooltip = new Tooltip(tooltipStr);
        tooltip.setShowDelay(Duration.millis(300));
        button.setTooltip(tooltip);

        return button;
    }

    private ToggleButton generateDebugToggleButton(String imageStr, String imageStrDisabled, String tooltipStr, String enabledNotif, String disabledNotif, boolean selected) {
        ToggleButton button = new ToggleButton();
        button.getStyleClass().add("secondary");
        button.setSelected(true);

        Image image = new Image(getClass().getClassLoader().getResourceAsStream("images/" + imageStr + ".png"));
        Image imageDisabled = new Image(getClass().getClassLoader().getResourceAsStream("images/" + imageStrDisabled + ".png"));
        ImageView imageView = new ImageView(image);
        button.setGraphic(imageView);

        button.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                imageView.setImage(image);
                Utils.addNotification(enabledNotif);
            }
            else {
                imageView.setImage(imageDisabled);
                Utils.addNotification(disabledNotif);
            }
        });

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
    private void toggleCaseSensitive() {
        this.caseSensitive = !this.caseSensitive;
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
        String originalSearch = findField.getText();

        if (search.isEmpty()) {
            Platform.runLater(() -> Utils.addNotification("Find text field cannot be empty"));
            return;
        }

        if (wrapSearch == +1) codeArea.moveTo(0);
        wrapSearch = 0;

        if (!caseSensitive) {
            text = text.toLowerCase();
            search = search.toLowerCase();
        }

        int start = -1;
        int end = -1;

        if (this.regex) {
            try {
                Matcher matcher = Pattern.compile(search).matcher(text);
                if (matcher.find()) {
                    boolean found = true;
                    while (matcher.group().isEmpty()) {
                        if (!matcher.find()) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        start = matcher.start();
                        end = matcher.end();
                    }
                }
            }
            catch (PatternSyntaxException e) {
                if (showAlert) {
                    codeArea.setEditable(true);
                    Platform.runLater(() -> Utils.addNotification("Bad regex pattern: " + originalSearch));
                    return;
                }
                else throw e;
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
            codeArea.scrollXToPixel(0);
            codeArea.requestFollowCaret();
        }
        else {
            wrapSearch = +1;

            if (showAlert) {
                Platform.runLater(() -> Utils.addNotification("Reached end of file, text not found"));
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
        String originalSearch = findField.getText();

        if (search.isEmpty()) {
            Platform.runLater(() -> Utils.addNotification("Find text field cannot be empty"));
            return;
        }

        if (wrapSearch == -1) codeArea.moveTo(codeArea.getText().length());
        wrapSearch = 0;

        if (!caseSensitive) {
            text = text.toLowerCase();
            search = search.toLowerCase();
        }

        int start = -1;
        int end = -1;

        if (this.regex) {
            try {
                Matcher matcher = Pattern.compile(search).matcher(text);
                while (matcher.find()) {
                    boolean found = true;
                    while (matcher.group().isEmpty()) {
                        if (!matcher.find()) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        start = matcher.start();
                        end = matcher.end();
                    }
                }
            }
            catch (PatternSyntaxException e) {
                codeArea.setEditable(true);
                Platform.runLater(() -> Utils.addNotification("Bad regex pattern: " + originalSearch));
                return;
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
            codeArea.scrollXToPixel(0);
            codeArea.requestFollowCaret();
        }
        else {
            wrapSearch = -1;

            if (showAlert) {
                Platform.runLater(() -> Utils.addNotification("Reached beginning of file, text not found"));
            }
        }
    }

    @FXML
    private void replace() {
        TabData tabData = currentTab;

        CodeArea codeArea = tabData.getCodeArea();

        if (!codeArea.isEditable()) {
            Utils.addNotification("Text replacement is disabled on this tab at the moment");
            return;
        }

        String search = findField.getText();
        String replace = replaceField.getText();

        if (search.isEmpty()) {
            Platform.runLater(() -> Utils.addNotification("Find text field cannot be empty"));
            return;
        }

        try {
            Pattern.compile(search);
        }
        catch (PatternSyntaxException e) {
            codeArea.setEditable(true);
            Platform.runLater(() -> Utils.addNotification("Bad regex pattern: " + search));
            return;
        }

        IndexRange range = codeArea.getSelection();
        String selectedText = codeArea.getSelectedText();
        if (range.getLength() == 0 ||
                ((caseSensitive && !selectedText.equals(search)) ||
                        (!caseSensitive && !selectedText.equalsIgnoreCase(search)))) {

            IndexRange selection = codeArea.getSelection();
            codeArea.moveTo(selection.getStart());

            findNext(false);

            range = codeArea.getSelection();
            if (range.getLength() == 0) {
                Platform.runLater(() -> Utils.addNotification("Reached end of file, text not found"));
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

        if (!codeArea.isEditable()) {
            Utils.addNotification("Text replacement is disabled on this tab at the moment");
            return;
        }

        EventHandler<ContextMenuEvent> consumeAllContextMenu = Event::consume;

        Thread thread = new Thread(() -> {

            String originalText = tabData.getFileText();
            String text = tabData.getFileText();

            String originalSearch = findField.getText();
            String search = findField.getText();

            String replace = replaceField.getText();

            if (search.isEmpty()) {
                Platform.runLater(() -> Utils.addNotification("Find text field cannot be empty"));
                return;
            }

            codeArea.setEditable(false);
            codeArea.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);

            AtomicReference<NotificationManager.Notification> notificationAtomicReference = new AtomicReference<>();
            Utils.runAndWait(() -> notificationAtomicReference.set(Utils.addNotificationWithProgress("Performing text replacement")));
            NotificationManager.Notification notification = notificationAtomicReference.get();

            AtomicBoolean kill = new AtomicBoolean(false);
            notification.addListener(() -> {
                kill.set(true);
            });

            int count = 0;

            if (!caseSensitive) {
                text = text.toLowerCase();
                search = search.toLowerCase();
            }

            StringBuilder builder = new StringBuilder(originalText);

            if (this.regex) {
                try {
                    int adjust = 0;
                    Matcher matcher = Pattern.compile(search).matcher(text);
                    while (matcher.find()) {
                        if (kill.get()) {
                            codeArea.setEditable(true);
                            codeArea.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);
                            Platform.runLater(() -> Utils.addNotification("Text replacement aborted"));
                            return;
                        }

                        int start = matcher.start();
                        int end = matcher.end();

                        count++;
                        builder.replace(start + adjust, end + adjust, replace);
                        adjust += replace.length() - (end - start);
                    }
                }
                catch (PatternSyntaxException e) {
                    codeArea.setEditable(true);
                    codeArea.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);
                    Platform.runLater(() -> {
                        notification.close();
                        Utils.addNotification("Bad regex pattern: " + originalSearch);
                    });
                    return;
                }
            } else {
                int adjust = 0;

                int start = builder.indexOf(search);
                int end = start + search.length();

                while (start != -1) {
                    if (kill.get()) {
                        codeArea.setEditable(true);
                        codeArea.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);
                        Platform.runLater(() -> Utils.addNotification("Text replacement aborted"));
                        return;
                    }

                    count++;
                    builder.replace(start + adjust, end + adjust, replace);
                    adjust += replace.length() - (end - start);

                    start = builder.indexOf(search, end);
                    end = start + search.length();
                }
            }

            if (count == 0) {
                Platform.runLater(() -> {
                    notification.close();
                    Utils.addNotification("Reached end of file, text not found");
                });
            }
            else {
                int finalCount = count;
                Platform.runLater(() -> {
                    codeArea.replaceText(0, codeArea.getLength(), builder.toString());
                    notification.close();
                    if (finalCount == 1) {
                        Utils.addNotification(finalCount + " occurrence of the text was replaced");
                    }
                    else {
                        Utils.addNotification(finalCount + " occurrences of the text was replaced");
                    }
                });
            }

            codeArea.setEditable(true);
            codeArea.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumeAllContextMenu);
        });
        thread.start();
    }

    @FXML
    private void toggleSearch() {
        boolean visible = !findAndReplace.isVisible();
        findAndReplace.setVisible(visible);
        searchButton.setSelected(visible);
    }

    @FXML
    private void settings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, ButtonType.APPLY, ButtonType.CANCEL);

        Utils.setDefaultButton(alert, ButtonType.APPLY);

        alert.setTitle(Constants.APPLICATION_NAME);
        alert.setHeaderText("Settings");

        StackPane imagePane = new StackPane();
        imagePane.setPadding(new Insets(8));
        ImageView imageView = new ImageView();
        Image image = new Image(getClass().getClassLoader().getResourceAsStream("images/settings-medium.png"));
        imageView.setImage(image);
        imagePane.getChildren().add(imageView);
        alert.setGraphic(imagePane);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        TitledPane interpreter = new TitledPane();
        interpreter.setText("Interpreter");
        interpreter.setCollapsible(false);

        interpreter.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setFillWidth(interpreter, true);
        GridPane.setFillHeight(interpreter, true);
        gridPane.add(interpreter, 0, 0);

        VBox vBox1 = new VBox();
        vBox1.setSpacing(15);
        interpreter.setContent(vBox1);

        CheckBox cellSize = new CheckBox("Use 16 bit cells (values range 0 - 65535)");
        cellSize.setSelected(Configuration.getCellSize() == 16);
        vBox1.getChildren().add(cellSize);

        HBox memorySizeBox = new HBox();
        memorySizeBox.setSpacing(10);
        memorySizeBox.setAlignment(Pos.CENTER);
        vBox1.getChildren().add(memorySizeBox);

        Label label = new Label("Memory size");
        memorySizeBox.getChildren().add(label);

        TextField memorySize = new TextField();
        memorySize.setPromptText("In range 1000 to 50000");
        HBox.setHgrow(memorySize, Priority.ALWAYS);
        memorySize.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                memorySize.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        memorySize.setText(String.valueOf(Configuration.getMemorySize()));
        memorySizeBox.getChildren().add(memorySize);

        TitledPane editing = new TitledPane();
        editing.setText("Code Editing");
        editing.setCollapsible(false);

        editing.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setFillWidth(editing, true);
        GridPane.setFillHeight(editing, true);
        GridPane.setRowSpan(editing, 2);
        gridPane.add(editing, 1, 0);

        VBox vBox2 = new VBox();
        vBox2.setSpacing(10);
        editing.setContent(vBox2);

        CheckBox wrapText = new CheckBox("Wrap text in code area");
        wrapText.setSelected(Configuration.getWrapText());
        vBox2.getChildren().add(wrapText);

        CheckBox autoComplete = new CheckBox("Automatically complete brackets [ ]");
        autoComplete.setSelected(Configuration.getAutoComplete());
        vBox2.getChildren().add(autoComplete);

        CheckBox syntaxHighlighting = new CheckBox("Highlight brainfuck syntax");
        syntaxHighlighting.setSelected(Configuration.getSyntaxHighlighting());
        vBox2.getChildren().add(syntaxHighlighting);

        CheckBox bracketHighlighting = new CheckBox("Highlight matching brackets");
        bracketHighlighting.setSelected(Configuration.getBracketHighlighting());
        vBox2.getChildren().add(bracketHighlighting);

        TitledPane miscellaneous = new TitledPane();
        miscellaneous.setText("Miscellaneous");
        miscellaneous.setCollapsible(false);

        miscellaneous.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setFillWidth(miscellaneous, true);
        GridPane.setFillHeight(miscellaneous, true);
        gridPane.add(miscellaneous, 0, 1);

        VBox vBox3 = new VBox();
        vBox3.setSpacing(10);
        miscellaneous.setContent(vBox3);

        CheckBox autoSave = new CheckBox("Automatically save files every few seconds");
        autoSave.setSelected(Configuration.getAutoSave());
        vBox3.getChildren().add(autoSave);

        CheckBox showTips = new CheckBox("Show tips at startup");
        showTips.setSelected(Configuration.getShowTips());
        vBox3.getChildren().add(showTips);

        alert.getDialogPane().setContent(gridPane);

        alert.initOwner(tabPane.getScene().getWindow());

        boolean valid = false;

        do {
            alert.setResult(null);
            alert.showAndWait();

            ButtonType buttonType = alert.getResult();
            if (buttonType == ButtonType.APPLY) {
                try {
                    Integer memory = Integer.valueOf(memorySize.getText());
                    if (memory < 1000 || memory > 50000) throw new NumberFormatException("Invalid memory size");
                    valid = true;
                }
                catch (NumberFormatException e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Memory size must be in range 1000 to 50000");
                    error.initOwner(tabPane.getScene().getWindow());
                    error.showAndWait();
                    valid = false;
                }
            }
            else break;
        }
        while (!valid);

        if (valid) {
            if (cellSize.isSelected()) Configuration.setCellSize(16);
            else Configuration.setCellSize(8);
            Configuration.setMemorySize(Integer.valueOf(memorySize.getText()));
            Configuration.setWrapText(wrapText.isSelected());
            Configuration.setAutoComplete(autoComplete.isSelected());
            Configuration.setSyntaxHighlighting(syntaxHighlighting.isSelected());
            Configuration.setBracketHighlighting(bracketHighlighting.isSelected());
            Configuration.setAutoSave(autoSave.isSelected());
            Configuration.setShowTips(showTips.isSelected());

            try {
                Configuration.flush();

                for (TabData tabData : tabDataList) {
                    tabData.getCodeArea().setWrapText(Configuration.getWrapText());

                    if (Configuration.getSyntaxHighlighting()) Highlighter.refreshHighlighting(tabData);
                    else Highlighter.clearHighlighting(tabData);

                    if (Configuration.getBracketHighlighting()) tabData.getBracketHighlighter().highlightBracket();
                    else tabData.getBracketHighlighter().clearBracket();
                }
            } catch (ConfigurationException | IOException e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Failed to save configuration!");
                error.initOwner(tabPane.getScene().getWindow());
                error.showAndWait();
            }
        }
    }

    @FXML
    private void about() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Constants.APPLICATION_NAME);

        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        alert.getDialogPane().lookupButton(ButtonType.OK).setManaged(false);

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

        Hyperlink hyperlink3 = new Hyperlink("Licensed under GPL v3.0");
        hyperlink3.getStyleClass().add("subheading4");
        hyperlink3.setOnAction(event -> {
            Utils.browseURL("https://github.com/prat-man/Brainfuck-IDE/blob/master/LICENSE");
        });
        vBox.getChildren().add(hyperlink3);

        alert.getDialogPane().setContent(vBox);

        alert.initOwner(tabPane.getScene().getWindow());
        alert.showAndWait();
    }

    @FXML
    private void keymapReference() {
        Utils.browseURL("https://github.com/prat-man/Brainfuck-IDE/blob/master/keymap_reference.pdf");
    }

    @FXML
    private void debug() {
        Thread thread = new Thread(() -> {
            synchronized (processLock) {
                TabData tabData = currentTab;

                Utils.runAndWait(() -> saveFile());

                if (tabData.getFilePath() != null) {
                    // stop and hide debugger
                    if (tabData.getDebugger() != null)
                        tabData.getDebugger().stop();
                    tabData.getDebug().setVisible(false);
                    tabData.getDebugTerminal().setVisible(false);

                    // stop and hide interpreter
                    if (tabData.getInterpreter() != null)
                        tabData.getInterpreter().stop();
                    tabData.getInterpretTerminal().setVisible(false);

                    // show and start debugger
                    tabData.initializeDebugger();
                    tabData.getDebug().setVisible(true);
                    tabData.getDebugTerminal().setVisible(true);
                    tabData.getTableView().scrollTo(0);
                    tabData.getTableView().getSelectionModel().select(0);
                    tabData.getDebugger().start();
                }
            }
        });
        thread.start();
    }

    @FXML
    private void interpret() {
        Thread thread = new Thread(() -> {
            synchronized (processLock) {
                TabData tabData = currentTab;

                Utils.runAndWait(() -> saveFile());

                if (tabData.getFilePath() != null) {
                    // stop and hide debugger
                    if (tabData.getDebugger() != null)
                        tabData.getDebugger().stop();
                    tabData.getDebug().setVisible(false);
                    tabData.getDebugTerminal().setVisible(false);

                    // stop and hide interpreter
                    if (tabData.getInterpreter() != null)
                        tabData.getInterpreter().stop();
                    tabData.getInterpretTerminal().setVisible(false);

                    // show and start interpreter
                    tabData.initializeInterpreter();
                    tabData.getInterpretTerminal().setVisible(true);
                    tabData.getInterpreter().start();
                }
            }
        });
        thread.start();
    }

    @FXML
    private void exportToC() {
        TabData tabData = currentTab;
        saveFile();
        if (tabData.getFilePath() != null) {
            CTranslator translator = new CTranslator(this.currentTab);
            translator.start();
        }
    }

    @FXML
    private void exportToJava() {
        TabData tabData = currentTab;
        saveFile();
        if (tabData.getFilePath() != null) {
            JavaTranslator translator = new JavaTranslator(this.currentTab);
            translator.start();
        }
    }

    @FXML
    private void exportToJavaFast() {
        TabData tabData = currentTab;
        saveFile();
        if (tabData.getFilePath() != null) {
            JavaTranslatorFast translator = new JavaTranslatorFast(this.currentTab);
            translator.start();
        }
    }

    @FXML
    private void exportToPython() {
        TabData tabData = currentTab;
        saveFile();
        if (tabData.getFilePath() != null) {
            PythonTranslator translator = new PythonTranslator(this.currentTab);
            translator.start();
        }
    }

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
        CodeArea codeArea = currentTab.getCodeArea();

        int chars = codeArea.getText().length();
        String charsStr = NumberFormat.getNumberInstance(Locale.US).format(chars);
        charCount.setText(charsStr + " characters");

        int lines = codeArea.getParagraphs().size();
        String linesStr = NumberFormat.getNumberInstance(Locale.US).format(lines);
        lineCount.setText(linesStr + " lines");
    }

    private void updateCaretStatus() {
        CodeArea codeArea = currentTab.getCodeArea();

        int pos = codeArea.getCaretPosition() + 1;
        int row = codeArea.getCurrentParagraph() + 1;
        int col = codeArea.getCaretColumn() + 1;

        String posStr = NumberFormat.getNumberInstance(Locale.US).format(pos);
        String rowStr = NumberFormat.getNumberInstance(Locale.US).format(row);
        String colStr = NumberFormat.getNumberInstance(Locale.US).format(col);

        caretPosition.setText("Position: " + posStr);
        caretRow.setText("Line: " + rowStr);
        caretColumn.setText("Column: " + colStr);
    }

}
