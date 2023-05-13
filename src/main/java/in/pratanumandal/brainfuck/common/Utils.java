package in.pratanumandal.brainfuck.common;

import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.BrainfuckStudioApplication;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import in.pratanumandal.brainfuck.tool.Number;
import in.pratanumandal.brainfuck.tool.Text;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
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

    public static void convertNumber(TabData currentTab) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, ButtonType.OK, ButtonType.CANCEL);

        Image image = new Image(Utils.class.getClassLoader().getResourceAsStream("images/tools.png"));
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
        alert.setHeaderText("Convert Number");

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER_LEFT);
        vBox.setSpacing(15);

        HBox numberBox = new HBox();
        numberBox.setSpacing(10);
        numberBox.setAlignment(Pos.CENTER_LEFT);
        vBox.getChildren().add(numberBox);

        Label numberLabel = new Label("Number");
        numberBox.getChildren().add(numberLabel);

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
        numberBox.getChildren().add(numberField);

        alert.getDialogPane().setContent(vBox);
        alert.getDialogPane().setPrefWidth(300);

        alert.initOwner(currentTab.getTab().getTabPane().getScene().getWindow());

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

    public static void convertText(TabData currentTab) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, null, ButtonType.OK, ButtonType.CANCEL);

        Image image = new Image(Utils.class.getClassLoader().getResourceAsStream("images/tools.png"));
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
        alert.setHeaderText("Convert Text");

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER_LEFT);
        vBox.setSpacing(15);

        TextArea textArea = new TextArea();
        textArea.setPromptText("Enter the text");
        textArea.prefWidthProperty().bind(vBox.widthProperty());
        textArea.prefHeightProperty().bind(vBox.heightProperty());
        vBox.getChildren().add(textArea);

        alert.getDialogPane().setContent(vBox);

        alert.setResizable(true);
        alert.getDialogPane().setPrefWidth(500);
        alert.getDialogPane().setPrefHeight(350);

        ((Stage) alert.getDialogPane().getScene().getWindow()).setMinWidth(350);
        ((Stage) alert.getDialogPane().getScene().getWindow()).setMinHeight(300);

        alert.initOwner(currentTab.getTab().getTabPane().getScene().getWindow());

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
