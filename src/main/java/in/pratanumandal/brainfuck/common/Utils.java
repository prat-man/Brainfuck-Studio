package in.pratanumandal.brainfuck.common;

import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.gui.Main;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.fxmisc.richtext.CodeArea;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
                    Utils.addNotificationWithDelay("Tip:\nYou can use ~ (tilde symbol)\nas breakpoints for debugging", 30000);
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
        Main.hostServices.showDocument(url);
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

    public static void setDockIconIfMac() {
        if (SystemUtils.IS_OS_MAC) {
            try {
                Class clazz = Utils.class.getClassLoader().loadClass("com.apple.eawt.Application");
                Method getApplication = clazz.getMethod("getApplication");
                Object object = getApplication.invoke(null);
                Method setDockImage = clazz.cast(object).getClass().getMethod("setDockIconImage", Image.class);

                URL iconURL = Utils.class.getClassLoader().getResource("images/icon-large.png");
                Image image = new ImageIcon(iconURL).getImage();

                setDockImage.invoke(object, image);
            } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
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

        alert.getDialogPane().getScene().getRoot().getStyleClass().add("gotoline-dialog");

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

        javafx.scene.control.Label label = new javafx.scene.control.Label("Line [ : column ]");
        lineNumberBox.getChildren().add(label);

        javafx.scene.control.TextField lineNumber = new TextField();
        lineNumber.setPromptText("Enter line number");
        HBox.setHgrow(lineNumber, Priority.ALWAYS);
        lineNumber.setText(currentLine + " : " + currentColumn);
        lineNumberBox.getChildren().add(lineNumber);

        alert.getDialogPane().setContent(vBox);

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

    public static Alert setDefaultButton(Alert alert, ButtonType defBtn) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes()) {
            ((Button) pane.lookupButton(t)).setDefaultButton(t == defBtn);
        }
        return alert;
    }

}
