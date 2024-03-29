package in.pratanumandal.brainfuck.common;

import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.BrainfuckStudioApplication;
import in.pratanumandal.brainfuck.gui.component.CodePad;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import in.pratanumandal.brainfuck.gui.highlight.Highlighter;
import in.pratanumandal.brainfuck.os.windows.WindowsUtils;
import in.pratanumandal.brainfuck.tool.Number;
import in.pratanumandal.brainfuck.tool.Text;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static NotificationManager notificationManager;

    public static void initializeNotificationManager(VBox notificationPane) {
        notificationManager = new NotificationManager(notificationPane);
    }

    public static NotificationManager.Notification addNotification(String text) {
        return notificationManager.addNotification(text);
    }

    public static NotificationManager.Notification addNotificationWithDelay(String text, Integer delay) {
        return notificationManager.addNotification(text, delay);
    }

    public static NotificationManager.Notification addNotificationWithProgress(String text) {
        return notificationManager.addNotificationWithProgress(text);
    }

    public static void showTips() {
        if (Configuration.getShowTips()) {
            Random random = new Random();
            int choice = Configuration.isFirstRun() ? 0 : random.nextInt(4);
            switch (choice) {
                case 0:
                    // show breakpoints notification
                    Utils.addNotificationWithDelay("Tip:\nYou can use # (hash symbol)\nas breakpoints for debugging", 30000);
                    break;

                case 1:
                    // show cell size notification
                    Utils.addNotificationWithDelay("Tip:\nYou can switch between 8 bit, 16 bit, and 32 bit cells from settings", 30000);
                    break;

                case 2:
                    // show memory size notification
                    Utils.addNotificationWithDelay("Tip:\nYou can change the size of interpreter memory from settings", 30000);
                    break;

                case 3:
                    // show memory wrap notification
                    Utils.addNotificationWithDelay("Tip:\nYou can change the memory to wrap on overflow from settings", 30000);
                    break;
            }
        }
    }

    public static void browseURL(String url) {
        BrainfuckStudioApplication.hostServices.showDocument(url);
    }

    public static void runAndWait(Runnable action) {
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
    }

    public static String nanoToBestFitTimeUnits(long nanoseconds) {
        DecimalFormat df = new DecimalFormat("0.00");

        double milliseconds = nanoseconds / 1_000_000.0;
        if (milliseconds < 1000) return df.format(milliseconds) + " milliseconds";
        else {
            double seconds = milliseconds / 1000.0;
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

    public static int[] truncate(int[] array, int newLength) {
        if (array.length < newLength) {
            return array;
        } else {
            int[] truncated = new int[newLength];
            System.arraycopy(array, 0, truncated, 0, newLength);
            return truncated;
        }
    }

    public static String join(char[] arr) {
        return new String(arr).chars().mapToObj(e -> "'" + (char) e + "'").collect(Collectors.joining(", "));
    }

    public static String join(int[] arr) {
        return Arrays.stream(arr).mapToObj(e -> String.valueOf(e)).collect(Collectors.joining(", "));
    }

    public static void bringToFront(Stage primaryStage) {
        primaryStage.setIconified(false);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setAlwaysOnTop(false);
        primaryStage.requestFocus();
    }

    public static void setTaskbarIcon() {
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();

            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                URL imageURL = Utils.class.getClassLoader().getResource("images/icon/icon_128.png");
                taskbar.setIconImage(toolkit.getImage(imageURL));
            }
        }
    }

    public static void setStageIcon(Stage stage) {
        stage.getIcons().addAll(
                new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_16.png")),
                new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_24.png")),
                new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_32.png")),
                new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_64.png")),
                new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_128.png")),
                new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_256.png")));
    }

    public static void setStylesheet(Stage stage) {
        stage.getScene().getStylesheets()
                .add(Utils.class.getClassLoader().getResource("css/brainfuck.css").toExternalForm());
    }

    private static int countNewlines(String text) {
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(text);
        int lines = 1;
        while (m.find()) {
            lines++;
        }
        return lines;
    }

    private static int calculateColumn(String text) {
        Matcher m = Pattern.compile(".*(\r\n|\r|\n)").matcher(text);
        int lineEnd = 0;
        while (m.find()) {
            lineEnd = m.end();
        }
        return text.length() - lineEnd;
    }

    public static void throwUnmatchedBracketException(String code, int pos) {
        String codeSlice = code.substring(0, pos - 1);
        int row = Utils.countNewlines(codeSlice);
        int col = Utils.calculateColumn(codeSlice) + 1;
        throw new UnmatchedBracketException("Unmatched bracket at position " + pos + " [" + row + " : " + col + "]");
    }

    public static void settings(Stage stage, List<TabData> tabDataList) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, ButtonType.APPLY, ButtonType.CANCEL);

        Utils.setDefaultButton(alert, ButtonType.APPLY);

        alert.setTitle(Constants.APPLICATION_NAME);
        alert.setHeaderText("Settings");

        Image image = new Image(Utils.class.getClassLoader().getResourceAsStream("images/settings.png"));
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitHeight(32);
        imageView.setFitWidth(32);
        StackPane imagePane = new StackPane();
        imagePane.setPadding(new Insets(5));
        imagePane.getChildren().add(imageView);
        alert.setGraphic(imagePane);

        TabPane tabPane = new TabPane();

        // execution
        Tab execution = new Tab("Execution");
        execution.setClosable(false);
        tabPane.getTabs().add(execution);

        VBox vBox1 = new VBox();
        vBox1.setSpacing(15);
        execution.setContent(vBox1);

        HBox cellSizeBox = new HBox();
        cellSizeBox.setSpacing(10);
        cellSizeBox.setAlignment(Pos.CENTER_LEFT);
        vBox1.getChildren().add(cellSizeBox);

        Label cellSizeLabel = new Label("Cell size (in bits)");
        cellSizeBox.getChildren().add(cellSizeLabel);

        ToggleGroup cellSizeGroup = new ToggleGroup();
        int selectedCellSize = Configuration.getCellSize();

        RadioButton cellSize8 = new RadioButton("8");
        cellSize8.setToggleGroup(cellSizeGroup);
        cellSize8.setSelected(selectedCellSize == 8);
        cellSizeBox.getChildren().add(cellSize8);

        RadioButton cellSize16 = new RadioButton("16");
        cellSize16.setToggleGroup(cellSizeGroup);
        cellSize16.setSelected(selectedCellSize == 16);
        cellSizeBox.getChildren().add(cellSize16);

        RadioButton cellSize32 = new RadioButton("32");
        cellSize32.setToggleGroup(cellSizeGroup);
        cellSize32.setSelected(selectedCellSize == 32);
        cellSizeBox.getChildren().add(cellSize32);

        HBox memorySizeBox = new HBox();
        memorySizeBox.setSpacing(10);
        memorySizeBox.setAlignment(Pos.CENTER_LEFT);
        vBox1.getChildren().add(memorySizeBox);

        Label memorySizeLabel = new Label("Memory size");
        memorySizeBox.getChildren().add(memorySizeLabel);

        TextField memorySize = new TextField();
        memorySize.setPromptText("In range 1000 to 50000");
        memorySize.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                memorySize.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        memorySize.setText(String.valueOf(Configuration.getMemorySize()));
        memorySizeBox.getChildren().add(memorySize);

        CheckBox wrapMemory = new CheckBox("Wrap memory pointer on overflow");
        wrapMemory.setSelected(Configuration.getWrapMemory());
        vBox1.getChildren().add(wrapMemory);

        // editor
        Tab editor = new Tab("Editor");
        editor.setClosable(false);
        tabPane.getTabs().add(editor);

        VBox vBox2 = new VBox();
        vBox2.setSpacing(10);
        editor.setContent(vBox2);

        CheckBox wrapText = new CheckBox("Wrap text in editor");
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

        CheckBox useSpaces = new CheckBox("Use spaces instead of tabs");
        useSpaces.setSelected(Configuration.getUseSpaces());
        vBox2.getChildren().add(useSpaces);

        // general
        Tab general = new Tab("General");
        general.setClosable(false);
        tabPane.getTabs().add(general);

        VBox vBox3 = new VBox();
        vBox3.setSpacing(10);
        general.setContent(vBox3);

        CheckBox autoSave = new CheckBox("Automatically save files every few seconds");
        autoSave.setSelected(Configuration.getAutoSave());
        vBox3.getChildren().add(autoSave);

        CheckBox checkUpdates = new CheckBox("Check for updates at startup");
        checkUpdates.setSelected(Configuration.getCheckUpdates());
        vBox3.getChildren().add(checkUpdates);

        CheckBox showTips = new CheckBox("Show tips at startup");
        showTips.setSelected(Configuration.getShowTips());
        vBox3.getChildren().add(showTips);

        Utils.setTabPaneLeftTabsHorizontal(tabPane);

        alert.getDialogPane().setContent(tabPane);

        alert.setResizable(true);

        alert.getDialogPane().setPrefSize(500, 350);

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.setMinWidth(450);
        alertStage.setMinHeight(350);

        WindowsUtils.setStageStyle(alertStage);

        alert.initOwner(stage);

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
                    WindowsUtils.setStageStyle((Stage) error.getDialogPane().getScene().getWindow());
                    error.initOwner(stage);
                    error.showAndWait();
                    valid = false;
                }
            }
            else break;
        }
        while (!valid);

        if (valid) {
            if (cellSize8.isSelected()) Configuration.setCellSize(8);
            else if (cellSize16.isSelected()) Configuration.setCellSize(16);
            else Configuration.setCellSize(32);
            Configuration.setMemorySize(Integer.valueOf(memorySize.getText()));
            Configuration.setWrapMemory(wrapMemory.isSelected());
            Configuration.setWrapText(wrapText.isSelected());
            Configuration.setAutoComplete(autoComplete.isSelected());
            Configuration.setSyntaxHighlighting(syntaxHighlighting.isSelected());
            Configuration.setBracketHighlighting(bracketHighlighting.isSelected());
            Configuration.setUseSpaces(useSpaces.isSelected());
            Configuration.setAutoSave(autoSave.isSelected());
            Configuration.setCheckUpdates(checkUpdates.isSelected());
            Configuration.setShowTips(showTips.isSelected());

            try {
                Configuration.flush();

                for (TabData tabData : tabDataList) {
                    tabData.getCodePad().setWrapText(Configuration.getWrapText());

                    if (Configuration.getSyntaxHighlighting()) Highlighter.refreshHighlighting(tabData);
                    else Highlighter.clearHighlighting(tabData);

                    if (Configuration.getBracketHighlighting()) tabData.getBracketHighlighter().highlightBracket();
                    else tabData.getBracketHighlighter().clearBracket();
                }
            } catch (ConfigurationException | IOException e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Failed to save configuration!");
                WindowsUtils.setStageStyle((Stage) error.getDialogPane().getScene().getWindow());
                error.initOwner(stage);
                error.showAndWait();
            }
        }
    }

    public static void setTabPaneLeftTabsHorizontal(TabPane tabPane){
        tabPane.setSide(Side.LEFT);
        tabPane.setRotateGraphic(true);

        int labelWidth = 80;

        for (Tab tab : tabPane.getTabs()) {
            Label label = new Label(tab.getText());
            label.setPrefWidth(labelWidth);
            label.setRotate(90);
            StackPane stackPane = new StackPane(new Group(label));
            tab.setGraphic(stackPane);
            tab.setText(null);
            tab.getContent().getStyleClass().add("tab-content");
        }

        tabPane.setTabMinHeight(labelWidth + 20);
        tabPane.setTabMaxHeight(labelWidth + 20);
    }

    public static void about(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Constants.APPLICATION_NAME);

        alert.setHeaderText(null);
        alert.setGraphic(null);
        Utils.hideButton(alert, ButtonType.OK);

        VBox vBox = new VBox();
        vBox.getStyleClass().add("about");
        vBox.setAlignment(Pos.CENTER);

        ImageView imageView1 = new ImageView();
        Image image1 = new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_128.png"));
        imageView1.setImage(image1);
        imageView1.setFitHeight(96);
        imageView1.setFitWidth(96);
        imageView1.getStyleClass().add("icon");
        vBox.getChildren().add(imageView1);

        Label label1 = new Label(Constants.APPLICATION_NAME + " " + Constants.APPLICATION_VERSION);
        label1.getStyleClass().add("title");
        vBox.getChildren().add(label1);

        Hyperlink hyperlink1 = new Hyperlink(Constants.WEBSITE_URL);
        hyperlink1.getStyleClass().add("hyperlink");
        hyperlink1.setOnAction(event -> {
            Utils.browseURL(Constants.WEBSITE_URL);
        });
        vBox.getChildren().add(hyperlink1);

        Label label2 = new Label("from");
        label2.getStyleClass().add("subheading1");
        vBox.getChildren().add(label2);

        Label label3 = new Label("Pratanu Mandal");
        label3.getStyleClass().add("subheading2");
        vBox.getChildren().add(label3);

        Hyperlink hyperlink2 = new Hyperlink(Constants.AUTHOR_URL);
        hyperlink2.getStyleClass().add("hyperlink");
        hyperlink2.setOnAction(event -> {
            Utils.browseURL(Constants.AUTHOR_URL);
        });
        vBox.getChildren().add(hyperlink2);

        Label label4 = new Label("with");
        label4.getStyleClass().add("subheading3");
        vBox.getChildren().add(label4);

        ImageView imageView2 = new ImageView();
        Image image2 = new Image(Utils.class.getClassLoader().getResourceAsStream("images/heart.png"));
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
            Utils.browseURL(Constants.LICENSE_URL);
        });
        vBox.getChildren().add(hyperlink3);

        alert.getDialogPane().setContent(vBox);
        alert.getDialogPane().getStyleClass().add("about-dialog");

        WindowsUtils.setStageStyle((Stage) alert.getDialogPane().getScene().getWindow());

        alert.initOwner(stage);
        alert.showAndWait();
    }

    public static void goToLine(TabData currentTab) {
        CodePad codePad = currentTab.getCodePad();

        int lineCount = codePad.getParagraphs().size();
        int currentLine = codePad.getCurrentParagraph() + 1;
        int currentColumn = codePad.getCaretColumn() + 1;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.OK, ButtonType.CANCEL);

        Utils.setDefaultButton(alert, ButtonType.OK);

        alert.setTitle(Constants.APPLICATION_NAME);
        alert.setHeaderText("Go to Line");

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER_LEFT);
        vBox.setSpacing(15);

        HBox lineNumberBox = new HBox();
        lineNumberBox.setSpacing(10);
        lineNumberBox.setAlignment(Pos.CENTER);
        vBox.getChildren().add(lineNumberBox);

        Label label = new Label("Line [ : column ]");
        lineNumberBox.getChildren().add(label);

        TextField lineNumber = new TextField();
        lineNumber.setPromptText("Enter line number");
        HBox.setHgrow(lineNumber, Priority.ALWAYS);
        lineNumber.setText(currentLine + " : " + currentColumn);
        lineNumberBox.getChildren().add(lineNumber);

        alert.getDialogPane().setContent(vBox);
        alert.getDialogPane().setMinWidth(300);

        WindowsUtils.setStageStyle((Stage) alert.getDialogPane().getScene().getWindow());

        alert.initOwner(currentTab.getTab().getTabPane().getScene().getWindow());

        alert.setOnShown(event -> Platform.runLater(() -> {
            lineNumber.requestFocus();
            lineNumber.selectAll();
        }));

        while (true) {
            alert.setResult(null);
            alert.showAndWait();

            ButtonType buttonType = alert.getResult();
            if (buttonType == ButtonType.OK) {
                try {
                    String[] data = lineNumber.getText().split(":");
                    if (data.length > 2) continue;

                    Integer line = Integer.valueOf(data[0].trim());
                    Integer column = data.length > 1 ? Integer.valueOf(data[1].trim()) : 0;

                    if (line < 1 || column < 0) continue;

                    if (line > lineCount) line = lineCount;

                    if (column == 0) column = 1;
                    else {
                        int columnCount = codePad.getParagraphLength(line - 1);
                        if (column > columnCount + 1) column = columnCount + 1;
                    }

                    codePad.moveTo(line - 1, column - 1);
                    codePad.requestFollowCaret();

                    break;
                }
                catch (NumberFormatException e) { }
            }
            else break;
        }
    }

    public static void generateNumber(TabData currentTab) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, ButtonType.OK, ButtonType.CANCEL);

        Image image = new Image(Utils.class.getClassLoader().getResourceAsStream("images/generate.png"));
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitHeight(32);
        imageView.setFitWidth(32);
        StackPane imagePane = new StackPane();
        imagePane.setPadding(new Insets(5));
        imagePane.getChildren().add(imageView);
        alert.setGraphic(imagePane);

        Utils.setDefaultButton(alert, ButtonType.OK);
        Utils.setButtonText(alert, ButtonType.OK, "Insert");

        alert.setTitle(Constants.APPLICATION_NAME);
        alert.setHeaderText("Generate");

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER_LEFT);
        vBox.setSpacing(15);

        Label label = new Label("Generate brainfuck code for a number");
        vBox.getChildren().add(label);

        TextField numberField = new TextField();
        numberField.setPromptText("Enter a number");
        HBox.setHgrow(numberField, Priority.ALWAYS);
        numberField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("(\\+|\\-)?\\d*")) {
                newVal = newVal.replaceAll("(?!^)\\D", "");
                newVal = newVal.replaceAll("^[^\\+\\-\\d]", "");
                numberField.setText(newVal);
            }
        });
        vBox.getChildren().add(numberField);

        alert.getDialogPane().setContent(vBox);
        alert.getDialogPane().setPrefWidth(300);

        WindowsUtils.setStageStyle((Stage) alert.getDialogPane().getScene().getWindow());

        alert.initOwner(currentTab.getTab().getTabPane().getScene().getWindow());

        alert.setOnShown(event -> Platform.runLater(() -> numberField.requestFocus()));

        while (true) {
            alert.setResult(null);
            alert.showAndWait();

            ButtonType buttonType = alert.getResult();
            if (buttonType == ButtonType.OK) {
                String text = numberField.getText();

                if (text.isBlank() || text.matches("\\+|\\-")) continue;

                Integer number = Integer.valueOf(text);

                String converted = Number.convertToBrainfuck(number);
                converted = Utils.formatBrainfuck(converted);

                currentTab.getCodePad().insertText(currentTab.getCodePad().getCaretPosition(), converted);
            }

            break;
        }
    }

    public static void generateText(TabData currentTab) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, ButtonType.OK, ButtonType.CANCEL);

        Image image = new Image(Utils.class.getClassLoader().getResourceAsStream("images/generate.png"));
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitHeight(32);
        imageView.setFitWidth(32);
        StackPane imagePane = new StackPane();
        imagePane.setPadding(new Insets(5));
        imagePane.getChildren().add(imageView);
        alert.setGraphic(imagePane);

        Utils.setDefaultButton(alert, ButtonType.OK);
        Utils.setButtonText(alert, ButtonType.OK, "Insert");

        alert.setTitle(Constants.APPLICATION_NAME);
        alert.setHeaderText("Generate");

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER_LEFT);
        vBox.setSpacing(15);

        Label label = new Label("Generate brainfuck code for a text");
        vBox.getChildren().add(label);

        TextArea textArea = new TextArea();
        textArea.setPromptText("Enter the text");
        textArea.prefWidthProperty().bind(vBox.widthProperty());
        textArea.prefHeightProperty().bind(vBox.heightProperty());
        vBox.getChildren().add(textArea);

        alert.getDialogPane().setContent(vBox);

        alert.setResizable(true);

        alert.getDialogPane().setPrefSize(500, 400);

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.setMinWidth(350);
        alertStage.setMinHeight(300);

        WindowsUtils.setStageStyle(alertStage);

        alert.initOwner(currentTab.getTab().getTabPane().getScene().getWindow());

        alert.setOnShown(event -> Platform.runLater(() -> textArea.requestFocus()));

        while (true) {
            alert.setResult(null);
            alert.showAndWait();

            ButtonType buttonType = alert.getResult();
            if (buttonType == ButtonType.OK) {
                String text = textArea.getText();

                if (text.isBlank()) continue;

                String converted = Text.convertToBrainfuck(text);
                converted = Utils.formatBrainfuck(converted);

                currentTab.getCodePad().insertText(currentTab.getCodePad().getCaretPosition(), converted);
            }

            break;
        }
    }

    private static Snippets.Snippet modifySnippet(Snippets.Snippet snippet, Stage stage) {
        boolean isNewSnippet = false;
        if (snippet == null) {
            isNewSnippet = true;
            snippet = new Snippets.Snippet();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, ButtonType.OK, ButtonType.CANCEL);

        Image image = new Image(Utils.class.getClassLoader().getResourceAsStream("images/snippets.png"));
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitHeight(32);
        imageView.setFitWidth(32);
        StackPane imagePane = new StackPane();
        imagePane.setPadding(new Insets(5));
        imagePane.getChildren().add(imageView);
        alert.setGraphic(imagePane);

        alert.setTitle(Constants.APPLICATION_NAME);

        if (isNewSnippet) {
            alert.setHeaderText("Create Snippet");
        }
        else {
            alert.setHeaderText("Modify Snippet");
        }

        Utils.setButtonText(alert, ButtonType.OK, "Save");

        VBox vBox = new VBox();
        vBox.setSpacing(10);

        // name
        TitledPane namePane = new TitledPane();
        namePane.setText("Name");
        namePane.setCollapsible(false);
        vBox.getChildren().add(namePane);

        HBox nameBox = new HBox();
        nameBox.setPadding(new Insets(7, 5, 5, 5));
        namePane.setContent(nameBox);

        TextField nameField = new TextField(snippet.getName());
        nameField.setPromptText("Enter name of snippet");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameBox.getChildren().add(nameField);

        // split pane
        SplitPane splitPane = new SplitPane();
        vBox.getChildren().add(splitPane);

        splitPane.getStyleClass().add("snippet");

        VBox.setVgrow(splitPane, Priority.ALWAYS);

        splitPane.setDividerPositions(0.3);

        // description
        TitledPane descriptionPane = new TitledPane();
        descriptionPane.setText("Description");
        descriptionPane.setCollapsible(false);
        splitPane.getItems().add(descriptionPane);

        descriptionPane.prefHeightProperty().bind(splitPane.heightProperty());
        HBox.setHgrow(descriptionPane, Priority.ALWAYS);

        HBox descriptionBox = new HBox();
        descriptionBox.setPadding(new Insets(7, 6, 7, 5));
        descriptionPane.setContent(descriptionBox);

        TextArea descriptionField = new TextArea(snippet.getDescription());
        descriptionField.setPromptText("Enter description of snippet");
        HBox.setHgrow(descriptionField, Priority.ALWAYS);
        descriptionBox.getChildren().add(descriptionField);

        // code
        TitledPane codePane = new TitledPane();
        codePane.setText("Code");
        codePane.setCollapsible(false);
        splitPane.getItems().add(codePane);

        codePane.prefHeightProperty().bind(splitPane.heightProperty());
        HBox.setHgrow(codePane, Priority.ALWAYS);

        HBox codeBox = new HBox();
        codeBox.setPadding(new Insets(7, 6, 7, 5));
        codePane.setContent(codeBox);

        TextArea codeField = new TextArea(snippet.getCode());
        codeField.setPromptText("Enter code of snippet");
        HBox.setHgrow(codeField, Priority.ALWAYS);
        codeBox.getChildren().add(codeField);
        codeField.getStyleClass().add("code");

        alert.getDialogPane().setContent(vBox);

        alert.setResizable(true);

        alert.getDialogPane().setPrefSize(600, 450);

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.setMinWidth(500);
        alertStage.setMinHeight(400);

        WindowsUtils.setStageStyle(alertStage);

        alert.initOwner(stage);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.OK) {
            snippet.setName(nameField.getText());
            snippet.setDescription(descriptionField.getText());
            snippet.setCode(codeField.getText());

            return snippet;
        }

        return null;
    }

    public static Snippets.Snippet showSnippets(Stage stage, boolean allowInsert) {
        Snippets snippets = Snippets.loadSnippets();
        FilteredList<Snippets.Snippet> filteredSnippets = new FilteredList<>(snippets.getSnippets());
        SortedList<Snippets.Snippet> sortedSnippets = new SortedList<>(filteredSnippets);

        ButtonType CREATE = new ButtonType("Create", ButtonBar.ButtonData.LEFT);
        ButtonType MODIFY = new ButtonType("Modify", ButtonBar.ButtonData.LEFT);
        ButtonType DELETE = new ButtonType("Delete", ButtonBar.ButtonData.LEFT);
        ButtonType INSERT = new ButtonType("Insert", ButtonBar.ButtonData.RIGHT);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, CREATE, MODIFY, DELETE, INSERT, ButtonType.CANCEL);

        Image image = new Image(Utils.class.getClassLoader().getResourceAsStream("images/snippets.png"));
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitHeight(32);
        imageView.setFitWidth(32);
        StackPane imagePane = new StackPane();
        imagePane.setPadding(new Insets(5));
        imagePane.getChildren().add(imageView);
        alert.setGraphic(imagePane);

        alert.setTitle(Constants.APPLICATION_NAME);
        alert.setHeaderText("Snippets");

        Utils.setDefaultButton(alert, INSERT);
        Utils.hideButton(alert, ButtonType.CANCEL);
        alert.getDialogPane().lookupButton(DELETE).pseudoClassStateChanged(PseudoClass.getPseudoClass("cancel"), true);

        VBox vBox = new VBox();
        vBox.setSpacing(15);

        TextField search = new TextField();
        vBox.getChildren().add(search);

        search.getStyleClass().add("search");
        search.setPromptText("Search name");

        search.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                filteredSnippets.setPredicate(null);
            }
            else {
                filteredSnippets.setPredicate(snippet ->
                        snippet.getName().toLowerCase().contains(search.getText().toLowerCase()));
            }
        });
        search.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE && !search.getText().isEmpty()) {
                event.consume();
                search.clear();
            }
        });

        TableView<Snippets.Snippet> tableView = new TableView<>();
        vBox.getChildren().add(tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        tableView.setEditable(true);

        tableView.setItems(sortedSnippets);

        // table cell factory
        Callback cellFactory = snippet -> new TableCell<Snippets.Snippet, String>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);

                if (empty || text == null) {
                    setText(null);
                }
                else {
                    text = text.replaceAll("\n", " ");
                    setText(text);
                }
            }
        };

        // name column
        TableColumn<Snippets.Snippet, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(cellFactory);
        nameCol.setMinWidth(100);
        nameCol.setPrefWidth(100);
        nameCol.getStyleClass().add("left");

        // description column
        TableColumn<Snippets.Snippet, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setCellFactory(cellFactory);
        descriptionCol.setMinWidth(100);
        descriptionCol.setPrefWidth(200);
        descriptionCol.getStyleClass().add("left");

        // code column
        TableColumn<Snippets.Snippet, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setCellFactory(cellFactory);
        codeCol.setMinWidth(100);
        codeCol.setPrefWidth(300);
        codeCol.getStyleClass().add("left");
        codeCol.getStyleClass().add("code");

        // table row factory
        tableView.setRowFactory(t -> {
            TableRow<Snippets.Snippet> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Snippets.Snippet snippet = tableView.getSelectionModel().getSelectedItem();
                    snippet = modifySnippet(snippet, stage);
                    if (snippet != null) {
                        Snippets.saveSnippets(snippets);
                    }
                }
            });
            return row;
        });

        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.getColumns().addAll(nameCol, descriptionCol, codeCol);

        sortedSnippets.comparatorProperty().bind(tableView.comparatorProperty());
        nameCol.setSortType(TableColumn.SortType.ASCENDING);
        tableView.getSortOrder().add(nameCol);
        tableView.sort();

        Label label = new Label("No snippets added");
        tableView.setPlaceholder(label);

        alert.getDialogPane().setContent(vBox);

        snippets.getSnippets().addListener((ListChangeListener<? super Snippets.Snippet>) change -> Snippets.saveSnippets(snippets));

        // disable insert, modify, and delete buttons if no row is selected
        alert.getDialogPane().lookupButton(INSERT).setDisable(true);
        alert.getDialogPane().lookupButton(MODIFY).setDisable(true);
        alert.getDialogPane().lookupButton(DELETE).setDisable(true);
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            alert.getDialogPane().lookupButton(INSERT).setDisable(!allowInsert || newVal == null);
            alert.getDialogPane().lookupButton(MODIFY).setDisable(newVal == null);
            alert.getDialogPane().lookupButton(DELETE).setDisable(newVal == null);
        });

        // handle close request
        alert.setOnCloseRequest(event -> {
            ButtonType result = alert.getResult();

            if (result == CREATE) {
                event.consume();
                alert.setResult(null);

                Snippets.Snippet snippet = modifySnippet(null, stage);
                if (snippet != null) {
                    snippets.getSnippets().add(snippet);
                    tableView.getSelectionModel().select(snippet);
                }
            }
            else if (result == MODIFY) {
                event.consume();
                alert.setResult(null);

                Snippets.Snippet snippet = tableView.getSelectionModel().getSelectedItem();
                snippet = modifySnippet(snippet, stage);
                if (snippet != null) {
                    Snippets.saveSnippets(snippets);
                }
            }
            else if (result == DELETE) {
                event.consume();
                alert.setResult(null);

                Alert deleteAlert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO);

                deleteAlert.setTitle(Constants.APPLICATION_NAME);
                deleteAlert.setHeaderText("Delete Snippet");
                deleteAlert.setContentText("Are you sure you want to delete the snippet?\n\n");

                WindowsUtils.setStageStyle((Stage) deleteAlert.getDialogPane().getScene().getWindow());

                deleteAlert.initOwner(stage);
                deleteAlert.showAndWait();

                if (deleteAlert.getResult() == ButtonType.YES) {
                    Snippets.Snippet snippet = tableView.getSelectionModel().getSelectedItem();
                    snippets.getSnippets().remove(snippet);
                }
            }
        });

        // enable escape button
        alert.getDialogPane().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                alert.close();
            }
        });

        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                tableView.getSelectionModel().clearSelection();
                alert.getDialogPane().lookupButton(CREATE).requestFocus();
            }
        });

        alert.setResizable(true);

        alert.getDialogPane().setPrefSize(600, 450);

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.setMinWidth(500);
        alertStage.setMinHeight(400);

        WindowsUtils.setStageStyle(alertStage);

        alert.initOwner(stage);
        alert.showAndWait();

        ButtonType result = alert.getResult();
        if (result == INSERT) {
            return tableView.getSelectionModel().getSelectedItem();
        }

        return null;
    }

    public static void checkUpdates(Stage stage, boolean silent) {
        Thread thread = new Thread(() -> {
            if (!silent) Platform.runLater(() -> Utils.addNotification("Checking for updates"));

            try {
                Release release = Release.getRelease();

                int major = release.getVersion().getMajor();
                int minor = release.getVersion().getMinor();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, uuuu");
                String date = release.getDate().format(formatter);

                if (major > Constants.MAJOR_VERSION || minor > Constants.MINOR_VERSION) {
                    Platform.runLater(() -> {
                        ButtonType WEBSITE = new ButtonType("Open Website", ButtonBar.ButtonData.LEFT);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, ButtonType.OK, ButtonType.CANCEL, WEBSITE);

                        alert.setTitle(Constants.APPLICATION_NAME);
                        alert.setHeaderText("Updates available");

                        Utils.hideButton(alert, ButtonType.CANCEL);

                        VBox vBox = new VBox();
                        vBox.setSpacing(15);

                        GridPane gridPane = new GridPane();
                        gridPane.setHgap(10);
                        vBox.getChildren().add(gridPane);

                        Label label1 = new Label("Current version:");
                        gridPane.add(label1, 0, 0);

                        Label label2 = new Label(Constants.APPLICATION_VERSION);
                        gridPane.add(label2, 1, 0);

                        Label label3 = new Label();
                        gridPane.add(label3, 0, 1);

                        Label label4 = new Label();
                        gridPane.add(label4, 1, 1);

                        Label label5 = new Label("Latest version:");
                        gridPane.add(label5, 0, 2);

                        Label label6 = new Label(major + "." + minor);
                        gridPane.add(label6, 1, 2);

                        Label label7 = new Label("Release date:");
                        gridPane.add(label7, 0, 3);

                        Label label8 = new Label(date);
                        gridPane.add(label8, 1, 3);

                        TextArea textArea = new TextArea(release.getNotes());
                        textArea.setEditable(false);
                        vBox.getChildren().add(textArea);

                        alert.getDialogPane().setContent(vBox);

                        // handle close request
                        alert.setOnCloseRequest(event -> {
                            ButtonType result = alert.getResult();

                            if (result == WEBSITE) {
                                event.consume();
                                alert.setResult(null);

                                Utils.browseURL(Constants.WEBSITE_URL);
                            }
                        });

                        // enable escape button
                        alert.getDialogPane().setOnKeyPressed(event -> {
                            if (event.getCode() == KeyCode.ESCAPE) {
                                alert.close();
                            }
                        });

                        WindowsUtils.setStageStyle((Stage) alert.getDialogPane().getScene().getWindow());

                        alert.initOwner(stage);
                        alert.showAndWait();
                    });
                }
                else {
                    if (!silent) Platform.runLater(() -> Utils.addNotification("No updates available"));
                }
            }
            catch (Exception e) {
                if (!silent) Platform.runLater(() -> Utils.addNotification("Failed to check for updates"));
            }
        });
        thread.start();
    }

    public static Alert setDefaultButton(Alert alert, ButtonType btnTyp) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes()) {
            ((Button) pane.lookupButton(t)).setDefaultButton(t == btnTyp);
        }
        return alert;
    }

    public static Alert setCancelButton(Alert alert, ButtonType btnTyp) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes()) {
            ((Button) pane.lookupButton(t)).setCancelButton(t == btnTyp);
        }
        return alert;
    }

    public static Alert hideButton(Alert alert, ButtonType btnTyp) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes()) {
            pane.lookupButton(t).setVisible(t != btnTyp);
            pane.lookupButton(t).setManaged(t != btnTyp);
        }
        return alert;
    }

    public static Alert setButtonText(Alert alert, ButtonType btnTyp, String text) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes()) {
            if (t == btnTyp) {
                ((Button) pane.lookupButton(t)).setText(text);
            }
        }
        return alert;
    }

    public static String formatBrainfuck(String brainfuck) {
        brainfuck = brainfuck.replaceAll("\\s+", "");
        brainfuck = brainfuck.replaceAll("(.{5})", "$1 ").trim();
        brainfuck = brainfuck.replaceAll("((?:\\S*\\s){9}.*?)\\s", "$1\n").trim();
        return brainfuck;
    }

    public static String minifyBrainfuck(String brainfuck) {
        return brainfuck.replaceAll("[^><+\\-.,\\[\\]]", "");
    }

}
