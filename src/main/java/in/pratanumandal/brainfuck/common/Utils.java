package in.pratanumandal.brainfuck.common;

import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.BrainfuckStudioApplication;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import in.pratanumandal.brainfuck.gui.highlight.Highlighter;
import in.pratanumandal.brainfuck.gui.windows.StageOps;
import in.pratanumandal.brainfuck.tool.Number;
import in.pratanumandal.brainfuck.tool.Text;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.fxmisc.richtext.CodeArea;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
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
            int choice = Configuration.isFirstRun() ? 0 : random.nextInt(3);
            switch (choice) {
                case 0:
                    // show breakpoints notification
                    Utils.addNotificationWithDelay("Tip:\nYou can use # (hash symbol)\nas breakpoints for debugging", 30000);
                    break;

                case 1:
                    // show cell size notification
                    Utils.addNotificationWithDelay("Tip:\nYou can switch between 8 bit cells and 16 bit cells from settings", 30000);
                    break;

                case 2:
                    // show memory size notification
                    Utils.addNotificationWithDelay("Tip:\nYou can change the size of interpreter memory from settings", 30000);
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

    public static String nanoToBestFitTimeUnits(long nano) {
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
                new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_256.png")),
                new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_512.png")));
    }

    public static void setStylesheet(Stage stage) {
        stage.getScene().getStylesheets()
                .add(Utils.class.getClassLoader().getResource("css/brainfuck.css").toExternalForm());
    }

    public static void setStyle(Stage stage) {
        Platform.runLater(() -> {
            StageOps.WindowHandle handle = StageOps.findWindowHandle(stage);
            StageOps.setCaptionColor(handle, Color.rgb(78, 83, 92));
        });
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

        Utils.setStyle((Stage) alert.getDialogPane().getScene().getWindow());

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
                    Utils.setStyle((Stage) error.getDialogPane().getScene().getWindow());
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
                Utils.setStyle((Stage) error.getDialogPane().getScene().getWindow());
                error.initOwner(stage);
                error.showAndWait();
            }
        }
    }

    public static void about(Stage stage) {
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
        Image image1 = new Image(Utils.class.getClassLoader().getResourceAsStream("images/icon/icon_128.png"));
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
            Utils.browseURL("https://github.com/prat-man/Brainfuck-Studio/blob/master/LICENSE");
        });
        vBox.getChildren().add(hyperlink3);

        alert.getDialogPane().setContent(vBox);
        alert.getDialogPane().getStyleClass().add("about-dialog");

        Utils.setStyle((Stage) alert.getDialogPane().getScene().getWindow());

        alert.initOwner(stage);
        alert.showAndWait();
    }

    public static void goToLine(TabData currentTab) {
        CodeArea codeArea = currentTab.getCodeArea();

        int lineCount = codeArea.getParagraphs().size();
        int currentLine = codeArea.getCurrentParagraph() + 1;
        int currentColumn = codeArea.getCaretColumn() + 1;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.OK, ButtonType.CANCEL);

        Utils.setDefaultButton(alert, ButtonType.OK);

        alert.setTitle(Constants.APPLICATION_NAME);
        alert.setHeaderText("Go to line");

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

        Utils.setStyle((Stage) alert.getDialogPane().getScene().getWindow());

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
                        int columnCount = codeArea.getParagraphLength(line - 1);
                        if (column > columnCount + 1) column = columnCount + 1;
                    }

                    codeArea.moveTo(line - 1, column - 1);
                    codeArea.requestFollowCaret();

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

        Utils.setStyle((Stage) alert.getDialogPane().getScene().getWindow());

        alert.initOwner(currentTab.getTab().getTabPane().getScene().getWindow());

        alert.setOnShown(event -> Platform.runLater(() -> numberField.requestFocus()));

        while (true) {
            alert.setResult(null);
            alert.showAndWait();

            ButtonType buttonType = alert.getResult();
            if (buttonType == ButtonType.OK) {
                try {
                    String text = numberField.getText();

                    if (text.matches("\\+|\\-")) continue;

                    Integer number = Integer.valueOf(text);

                    String converted = Number.convertToBrainfuck(number);
                    converted = Utils.formatBrainfuck(converted);

                    currentTab.getCodeArea().insertText(currentTab.getCodeArea().getCaretPosition(), converted);

                    break;
                } catch (NumberFormatException e) { }
            }
            else break;
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
        alert.getDialogPane().setPrefWidth(500);
        alert.getDialogPane().setPrefHeight(400);

        ((Stage) alert.getDialogPane().getScene().getWindow()).setMinWidth(350);
        ((Stage) alert.getDialogPane().getScene().getWindow()).setMinHeight(300);

        Utils.setStyle((Stage) alert.getDialogPane().getScene().getWindow());

        alert.initOwner(currentTab.getTab().getTabPane().getScene().getWindow());

        alert.setOnShown(event -> Platform.runLater(() -> textArea.requestFocus()));

        while (true) {
            alert.setResult(null);
            alert.showAndWait();

            ButtonType buttonType = alert.getResult();
            if (buttonType == ButtonType.OK) {
                try {
                    String text = textArea.getText();

                    String converted = Text.convertToBrainfuck(text);
                    converted = Utils.formatBrainfuck(converted);

                    currentTab.getCodeArea().insertText(currentTab.getCodeArea().getCaretPosition(), converted);

                    break;
                } catch (NumberFormatException e) { }
            }
            else break;
        }
    }

    public static Alert setDefaultButton(Alert alert, ButtonType btnTyp) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes()) {
            ((Button) pane.lookupButton(t)).setDefaultButton(t == btnTyp);
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

}
