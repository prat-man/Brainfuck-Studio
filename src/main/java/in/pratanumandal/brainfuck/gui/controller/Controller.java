package in.pratanumandal.brainfuck.gui.controller;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.Memory;
import in.pratanumandal.brainfuck.engine.processor.translator.CTranslator;
import in.pratanumandal.brainfuck.engine.processor.translator.JavaTranslator;
import in.pratanumandal.brainfuck.engine.processor.translator.PythonTranslator;
import in.pratanumandal.brainfuck.gui.codearea.CustomCodeArea;
import in.pratanumandal.brainfuck.gui.codearea.FXTerminal;
import in.pratanumandal.brainfuck.gui.component.DefaultContextMenu;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import in.pratanumandal.brainfuck.gui.highlight.BracketHighlighter;
import in.pratanumandal.brainfuck.gui.highlight.Highlighter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Controller {

    private final List<TabData> tabDataList = new ArrayList<>();

    private final ChangeListener charCountListener = (ChangeListener<String>) (observableValue, oldValue, newValue) -> { updateTextStatus(); };
    private final ChangeListener caretPositionListener = (ChangeListener<Integer>) (observableValue, oldValue, newValue) -> { updateCaretStatus(); };

    private TabData currentTab;

    private int fontSize;

    private Stage stage;

    private boolean regex;
    private boolean caseSensitive;

    private int wrapSearch;

    private final Object processLock = new Object();

    @FXML private TabPane tabPane;
    @FXML private VBox placeHolder;

    @FXML private SplitMenuButton saveFileButton;
    @FXML private Button debugButton;
    @FXML private Button interpretButton;
    @FXML private MenuButton exportButton;

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
                Path path = Paths.get(Constants.WELCOME_FILE);

                Files.copy(getClass().getClassLoader().getResourceAsStream("bf/welcome.bf"), path, StandardCopyOption.REPLACE_EXISTING);

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
            }
        }

        // allow reordering of tabs
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        // change font size based on selection
        fontSizeChooser.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> observableValue, String oldVal, String newVal) -> {
            fontSize = Integer.valueOf(newVal.substring(0, newVal.length() - 2));

            Configuration.setFontSize(fontSize);
            try {
                Configuration.flush();
            } catch (ConfigurationException | IOException e) {
                // DO NOTHING HERE
                e.printStackTrace();
            }

            for (TabData td : tabDataList) {
                this.setFontSize(td);
            }
        });

        // select current font
        fontSize = Configuration.getFontSize();
        fontSizeChooser.getSelectionModel().select(fontSize + "px");
        for (TabData td : tabDataList) {
            this.setFontSize(td);
        }

        // toggle search
        searchButton.selectedProperty().addListener((obs, oldVal, newVal) -> findAndReplace.setVisible(newVal));
        searchButton.disableProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                searchButton.setSelected(false);
            }
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

        // create binding for checking if tabs are empty
        BooleanBinding emptyTabPaneBinding = Bindings.isEmpty(tabPane.getTabs());

        // show placeholder if no tabs are open
        placeHolder.visibleProperty().bind(emptyTabPaneBinding);
        placeHolder.managedProperty().bind(emptyTabPaneBinding);

        // disable buttons if no tabs are open
        saveFileButton.disableProperty().bind(emptyTabPaneBinding);
        debugButton.disableProperty().bind(emptyTabPaneBinding);
        interpretButton.disableProperty().bind(emptyTabPaneBinding);
        exportButton.disableProperty().bind(emptyTabPaneBinding);
        searchButton.disableProperty().bind(emptyTabPaneBinding);

        // update status if no tabs are open
        emptyTabPaneBinding.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                this.updateTextStatus();
                this.updateCaretStatus();
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        // define key events
        final KeyCombination keyComb1 = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
        final KeyCombination keyComb2 = new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN);
        final KeyCombination keyComb3 = new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN);
        final KeyCombination keyComb4 = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
        final KeyCombination keyComb5 = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
        final KeyCombination keyComb6 = new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);
        final KeyCombination keyComb7 = new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN);
        final KeyCombination keyComb8 = new KeyCodeCombination(KeyCode.F3);
        final KeyCombination keyComb9 = new KeyCodeCombination(KeyCode.F3, KeyCombination.SHIFT_DOWN);
        final KeyCombination keyComb10 = new KeyCodeCombination(KeyCode.F9);
        final KeyCombination keyComb11 = new KeyCodeCombination(KeyCode.F9, KeyCombination.SHIFT_DOWN);
        final KeyCombination keyComb12 = new KeyCodeCombination(KeyCode.F8);
        final KeyCombination keyComb13 = new KeyCodeCombination(KeyCode.F8, KeyCombination.SHIFT_DOWN);

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
            else if (this.currentTab != null && this.currentTab.getDebugger() != null) {
                if (keyComb10.match(event)) {
                    this.currentTab.getDebugger().resume();
                }
                else if (keyComb11.match(event)) {
                    this.currentTab.getDebugger().pause();
                }
                else if (keyComb12.match(event)) {
                    this.currentTab.getDebugger().step();
                }
                else if (keyComb13.match(event)) {
                    boolean selected = this.currentTab.getDebugBreakpointButton().isSelected();
                    this.currentTab.getDebugBreakpointButton().setSelected(!selected);
                }
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
        debugTerminalToolbar.getChildren().add(new VirtualizedScrollPane<>(debugTerminal));

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
                Platform.runLater(() -> {
                    splitPane.getItems().add(debugTerminalToolbar);
                    splitPane.setDividerPositions(tabData.getDividerPosition());
                });
            }
            else {
                Platform.runLater(() -> {
                    tabData.setDividerPosition(tabData.getSplitPane().getDividerPositions()[0]);
                    splitPane.getItems().remove(debugTerminalToolbar);
                });
            }
        });

        // create a debug side pane
        VBox debug = new VBox();
        debug.setMinWidth(250);
        debug.setMaxWidth(400);

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

        Slider debugSpeed = new Slider(0, 500, 350);
        debugSpeed.setMajorTickUnit(1);
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
        TableColumn<Memory, Boolean> column1 = new TableColumn<>("");
        column1.setCellValueFactory(new PropertyValueFactory<>("current"));
        column1.setCellFactory(column -> {
            // load pointer image
            Image pointer = new Image(getClass().getClassLoader().getResourceAsStream("images/pointer.png"));
            ImageView pointerNode = new ImageView(pointer);
            pointerNode.setFitHeight(16);
            pointerNode.setFitWidth(16);

            TableCell<Memory, Boolean> cell = new TableCell<>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setGraphic(null);
                    else if (item) setGraphic(pointerNode);
                    else setGraphic(null);
                }
            };
            return cell;
        });
        column1.setSortable(false);
        column1.setMinWidth(30);
        column1.setMaxWidth(35);
        tableView.getColumns().add(column1);

        TableColumn<Memory, Integer> column2 = new TableColumn<>("Address");
        column2.setCellValueFactory(new PropertyValueFactory<>("address"));
        column2.setCellFactory(column -> {
            TableCell<Memory, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    String format1 = "%0" + (int) (Math.log10(tableView.getItems().size()) + 1) + "d";
                    super.updateItem(item, empty);
                    if (empty) setText(null);
                    else setText(String.format(format1, item));
                }
            };
            return cell;
        });
        column2.setSortable(false);
        tableView.getColumns().add(column2);

        TableColumn<Memory, Integer> column3 = new TableColumn<>("Data");
        column3.setCellValueFactory(new PropertyValueFactory<>("data"));
        String format2 = "%03d";
        column3.setCellFactory(column -> {
            TableCell<Memory, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText(null);
                    else setText(String.format(format2, item));
                }
            };
            return cell;
        });
        column3.setSortable(false);
        tableView.getColumns().add(column3);

        TableColumn<Memory, Character> column4 = new TableColumn<>("Character");
        column4.setCellValueFactory(new PropertyValueFactory<>("character"));
        column4.setSortable(false);
        tableView.getColumns().add(column4);

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
        stopImageView.setFitHeight(16);
        stopImageView.setFitWidth(16);
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
        closeImageView.setFitHeight(16);
        closeImageView.setFitWidth(16);
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
        FXTerminal interpretTerminal = new FXTerminal();

        // set the interpreter terminal
        tabData.setInterpretTerminal(interpretTerminal);

        // add interpreter terminal toolbar and interpreter terminal to vbox
        VBox interpreterTerminalToolbar = new VBox();
        interpreterTerminalToolbar.getChildren().add(interpreterTerminalControls);
        interpreterTerminalToolbar.getChildren().add(new VirtualizedScrollPane<>(interpretTerminal));

        // set style of interpreter toolbar
        interpreterTerminalToolbar.getStyleClass().add("terminal-toolbar");

        // bind interpreter terminal height property
        interpretTerminal.prefHeightProperty().bind(interpreterTerminalToolbar.heightProperty());

        // bind interpreter terminal visibility
        interpretTerminal.setVisible(false);
        interpreterTerminalToolbar.setVisible(false);
        interpreterTerminalToolbar.managedProperty().bind(interpretTerminal.visibleProperty());
        interpreterTerminalToolbar.visibleProperty().bind(interpretTerminal.visibleProperty());
        interpretTerminal.managedProperty().bind(interpretTerminal.visibleProperty());
        interpretTerminal.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> {
                    splitPane.getItems().add(interpreterTerminalToolbar);
                    splitPane.setDividerPositions(tabData.getDividerPosition());
                });
            }
            else {
                Platform.runLater(() -> {
                    tabData.setDividerPosition(tabData.getSplitPane().getDividerPositions()[0]);
                    splitPane.getItems().remove(interpreterTerminalToolbar);
                });
            }
        });

        // add stop button action
        interpreterStopButton.setOnAction(actionEvent -> tabData.getInterpreter().stop());

        // add close button action
        interpreterCloseButton.setOnAction(actionEvent -> interpretTerminal.setVisible(false));

        // set tab content
        tab.setContent(horizontalSplitPane);

        // add tab data to tab data list
        tabDataList.add(tabData);

        // set font sizes
        this.setFontSize(tabData);

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
                    currentTab.getCodeArea().caretPositionProperty().removeListener(caretPositionListener);
                }

                currentTab = tabData;
                updateTextStatus();
                updateCaretStatus();

                currentTab.getCodeArea().textProperty().addListener(charCountListener);
                currentTab.getCodeArea().caretPositionProperty().addListener(caretPositionListener);
            }
        });

        return tabData;
    }

    private Button generateDebugButton(String imageStr, String tooltipStr) {
        Button button = new Button();
        button.getStyleClass().add("secondary");

        Image image = new Image(getClass().getClassLoader().getResourceAsStream("images/" + imageStr + ".png"));
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
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
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
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

        fileChooser.setInitialDirectory(Configuration.getInitialDirectory());

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Brainfuck files (*.bf, *.b)", "*.bf", "*.b");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(tabPane.getScene().getWindow());

        if (file != null) {
            Configuration.setInitialDirectory(file.getParentFile());
            try {
                Configuration.flush();
            } catch (ConfigurationException | IOException e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Failed to save configuration!");
                error.initOwner(tabPane.getScene().getWindow());
                error.showAndWait();
            }

            try {
                String filePath = file.getAbsolutePath();

                for (TabData data : tabDataList) {
                    if (filePath.equals(data.getFilePath())) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(Constants.APPLICATION_NAME);
                        alert.setContentText("The file is already open in Brainfuck Studio!\n\n");

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
        if (tabPane.getTabs().isEmpty()) return;

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
        if (tabPane.getTabs().isEmpty()) return;

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(Constants.APPLICATION_NAME);

        fileChooser.setInitialDirectory(Configuration.getInitialDirectory());

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Brainfuck files (*.bf, *.b)", "*.bf", "*.b");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());

        if (file != null) {
            Configuration.setInitialDirectory(file.getParentFile());
            try {
                Configuration.flush();
            } catch (ConfigurationException | IOException e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Failed to save configuration!");
                error.initOwner(tabPane.getScene().getWindow());
                error.showAndWait();
            }

            TabData tabData = currentTab;

            String filePath = file.getAbsolutePath();
            String fileText = tabData.getFileText();

            for (TabData data : tabDataList) {
                if (data != tabData && filePath.equals(data.getFilePath())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(Constants.APPLICATION_NAME);
                    alert.setContentText("The file is already open in Brainfuck Studio!\n\n");

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

        Image image = new Image(getClass().getClassLoader().getResourceAsStream("images/settings.png"));
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitHeight(32);
        imageView.setFitWidth(32);
        StackPane imagePane = new StackPane();
        imagePane.setPadding(new Insets(5));
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

        HBox cellSizeBox = new HBox();
        cellSizeBox.setSpacing(10);
        cellSizeBox.setAlignment(Pos.CENTER_LEFT);
        vBox1.getChildren().add(cellSizeBox);

        Label cellSizeLabel = new Label("Cell size");
        cellSizeBox.getChildren().add(cellSizeLabel);

        ToggleGroup cellSizeGroup = new ToggleGroup();
        int selectedCellSize = Configuration.getCellSize();

        RadioButton cellSize8 = new RadioButton("8 bits");
        cellSize8.setToggleGroup(cellSizeGroup);
        cellSize8.setSelected(selectedCellSize == 8);
        cellSizeBox.getChildren().add(cellSize8);

        RadioButton cellSize16 = new RadioButton("16 bits");
        cellSize16.setToggleGroup(cellSizeGroup);
        cellSize16.setSelected(selectedCellSize == 16);
        cellSizeBox.getChildren().add(cellSize16);

        HBox memorySizeBox = new HBox();
        memorySizeBox.setSpacing(10);
        memorySizeBox.setAlignment(Pos.CENTER_LEFT);
        vBox1.getChildren().add(memorySizeBox);

        Label memorySizeLabel = new Label("Memory size");
        memorySizeBox.getChildren().add(memorySizeLabel);

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
            if (cellSize8.isSelected()) Configuration.setCellSize(8);
            else Configuration.setCellSize(16);
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
        Image image1 = new Image(getClass().getClassLoader().getResourceAsStream("images/icon/icon_128.png"));
        imageView1.setImage(image1);
        imageView1.setFitHeight(96);
        imageView1.setFitWidth(96);
        imageView1.getStyleClass().add("icon");
        vBox.getChildren().add(imageView1);

        Label label1 = new Label(Constants.APPLICATION_NAME + " " + Constants.APPLICATION_VERSION);
        label1.getStyleClass().add("title");
        vBox.getChildren().add(label1);

        Hyperlink hyperlink1 = new Hyperlink("https://brainfuck.pratanumandal.in/");
        hyperlink1.getStyleClass().add("hyperlink");
        hyperlink1.setOnAction(event -> {
            Utils.browseURL("https://prat-man.github.io/Brainfuck-Studio/");
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
        imageView2.setFitWidth(48);
        imageView2.setFitHeight(48);
        vBox.getChildren().add(imageView2);

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        vBox.getChildren().add(hBox);

        Hyperlink hyperlink3 = new Hyperlink("Licensed under GPL v3.0");
        hyperlink3.getStyleClass().add("subheading4");
        hyperlink3.setOnAction(event -> {
            Utils.browseURL("https://github.com/prat-man/Brainfuck-Studio/blob/master/LICENSE");
        });
        vBox.getChildren().add(hyperlink3);

        alert.getDialogPane().setContent(vBox);
        alert.getDialogPane().getStyleClass().add("about-dialog");

        alert.initOwner(tabPane.getScene().getWindow());
        alert.showAndWait();
    }

    @FXML
    private void keymapReference() {
        Utils.browseURL("https://raw.githubusercontent.com/prat-man/Brainfuck-Studio/master/res/keymap/Brainfuck_Studio_Keymap_Reference.pdf");
    }

    @FXML
    private void debug() {
        if (tabPane.getTabs().isEmpty()) return;

        Thread thread = new Thread(() -> {
            synchronized (processLock) {
                TabData tabData = currentTab;

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
        });
        thread.start();
    }

    @FXML
    private void interpret() {
        if (tabPane.getTabs().isEmpty()) return;

        Thread thread = new Thread(() -> {
            synchronized (processLock) {
                TabData tabData = currentTab;

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
        });
        thread.start();
    }

    @FXML
    private void exportToC() {
        if (tabPane.getTabs().isEmpty()) return;

        CTranslator translator = new CTranslator(this.currentTab);
        translator.start();
    }

    @FXML
    private void exportToJava() {
        if (tabPane.getTabs().isEmpty()) return;

        JavaTranslator translator = new JavaTranslator(this.currentTab);
        translator.start();
    }

    @FXML
    private void exportToPython() {
        if (tabPane.getTabs().isEmpty()) return;

        PythonTranslator translator = new PythonTranslator(this.currentTab);
        translator.start();
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
        if (tabPane.getTabs().isEmpty()) {
            charCount.setText("Characters");
            lineCount.setText("Lines");
        }
        else {
            CodeArea codeArea = currentTab.getCodeArea();

            int chars = codeArea.getText().length();
            String charsStr = NumberFormat.getNumberInstance(Locale.US).format(chars);
            charCount.setText(charsStr + " characters");

            int lines = codeArea.getParagraphs().size();
            String linesStr = NumberFormat.getNumberInstance(Locale.US).format(lines);
            lineCount.setText(linesStr + " lines");
        }
    }

    private void updateCaretStatus() {
        if (tabPane.getTabs().isEmpty()) {
            caretPosition.setText("Position");
            caretRow.setText("Line");
            caretColumn.setText("Column");
        }
        else {
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

    private void setFontSize(TabData tabData) {
        tabData.getCodeArea().setStyle("-fx-font-size: " + fontSize + "px");
        tabData.getDebugTerminal().setStyle("-fx-font-size: " + fontSize + "px");
        tabData.getInterpretTerminal().setStyle("-fx-font-size: " + fontSize + "px");
    }

    public boolean exitApplication() {
        List<TabData> originalTabDataList = tabDataList.stream().toList();
        List<Node> closeList = tabPane.lookupAll(".tab-close-button").stream().toList();

        for (int i = 0; i < originalTabDataList.size(); i++) {
            TabData tabData = originalTabDataList.get(i);
            Tab tab = tabData.getTab();
            tabPane.getSelectionModel().select(tab);

            Node close = closeList.get(i);
            close.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED,
                    close.getLayoutX(), close.getLayoutY(),
                    close.getLayoutX(), close.getLayoutY(),
                    MouseButton.PRIMARY, 1,
                    true, true, true, true, true, true, true, true, true, true,
                    null));

            if (tabDataList.contains(tabData)) break;
        }

        return tabDataList.isEmpty();
    }

}
