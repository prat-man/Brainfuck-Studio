package in.pratanumandal.brainfuck.common;

import in.pratanumandal.brainfuck.gui.Main;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;

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
                    // show tilde notification
                    Utils.addNotificationWithDelay("Tip:\nYou can use ~ (tilde symbol)\nas breakpoints for debugging", 30000);
                    break;

                case 1:
                    // show tilde notification
                    Utils.addNotificationWithDelay("Tip:\nYou can switch between 8 bit cells and 16 bit cells from settings", 30000);
                    break;

                case 2:
                    // show tilde notification
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

    public static int countNewlines(String text) {
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(text);
        int lines = 1;
        while (m.find()) {
            lines++;
        }
        return lines;
    }

    public static int calculateColumn(String text) {
        Matcher m = Pattern.compile(".*(\r\n|\r|\n)").matcher(text);
        int lineEnd = 0;
        while (m.find()) {
            lineEnd = m.end();
        }
        return text.length() - lineEnd;
    }

}
