package in.pratanumandal.brainfuck.gui.component;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationManager {

    private final VBox notificationPane;

    public NotificationManager(VBox notificationPane) {
        this.notificationPane = notificationPane;
    }

    public Notification addNotification(String text) {
        return addNotification(text, 5000, false);
    }

    public Notification addNotification(String text, Integer delay) {
        return addNotification(text, delay, false);
    }

    public Notification addNotificationWithProgress(String text) {
        return addNotification(text, null, true);
    }

    private Notification addNotification(String text, Integer delay, boolean progress) {
        Notification notification = new Notification();

        HBox hBox = new HBox(10);
        hBox.getStyleClass().add("notification");
        notification.hBox = hBox;

        VBox vBox = new VBox(10);
        hBox.getChildren().add(vBox);

        Label label = new Label(text);
        label.setWrapText(true);
        vBox.getChildren().add(label);

        if (progress) {
            ProgressBar progressBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
            vBox.getChildren().add(progressBar);
            notification.progressBar = progressBar;
        }

        Image closeImage = new Image(getClass().getClassLoader().getResourceAsStream("images/close-large.png"));
        ImageView closeImageView = new ImageView(closeImage);
        closeImageView.setFitHeight(8);
        closeImageView.setFitWidth(8);

        Button close = new Button();
        close.setGraphic(closeImageView);
        close.getStyleClass().add("close");
        hBox.getChildren().add(close);

        close.setOnAction(event -> {
            this.notificationPane.getChildren().remove(hBox);
            for (NotificationListener listener : notification.listeners) {
                listener.closed();
            }
        });

        this.notificationPane.getChildren().add(hBox);

        if (delay != null) {
            new Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> notificationPane.getChildren().remove(hBox));
                        }
                    },
                    delay
            );
        }

        return notification;
    }

    public class Notification {

        private HBox hBox;
        private ProgressBar progressBar;

        private List<NotificationListener> listeners;

        public Notification() {
            this.listeners = new ArrayList<>();
        }

        public void addListener(NotificationListener listener) {
            this.listeners.add(listener);
        }

        public void setProgress(double value) {
            if (progressBar != null) progressBar.setProgress(value);
        }

        public void close() {
            notificationPane.getChildren().remove(hBox);
            for (NotificationListener listener : listeners) {
                listener.closed();
            }
        }

    }

    public interface NotificationListener {
        void closed();
    }

}
