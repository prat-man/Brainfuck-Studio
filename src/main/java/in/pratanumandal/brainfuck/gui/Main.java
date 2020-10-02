package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import tk.pratanumandal.unique4j.Unique4j;
import tk.pratanumandal.unique4j.Unique4jList;
import tk.pratanumandal.unique4j.exception.Unique4jException;

import java.util.Arrays;
import java.util.List;

public class Main extends Application {

    public static Stage superStage;
    public static HostServices hostServices;

    private static void loadFonts() {
        Font.loadFont(Main.class.getClassLoader().getResourceAsStream("fonts/OpenSans-Regular.ttf"), 12);
        Font.loadFont(Main.class.getClassLoader().getResourceAsStream("fonts/VeraMono.ttf"), 14);
    }

    @Override
    public void init() throws Exception {
        super.init();

        // do this before anything else
        Utils.setDockIconIfMac();

        hostServices = getHostServices();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        superStage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/main.fxml"));
        Parent root = loader.load();

        Controller controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.setTitle(Constants.APPLICATION_NAME);
        primaryStage.setScene(new Scene(root, 780, 500));
        primaryStage.setMinWidth(550);
        primaryStage.setMinHeight(350);
        primaryStage.sizeToScene();

        primaryStage.getScene().getStylesheets().add(getClass().getClassLoader().getResource("css/brainfuck.css").toExternalForm());
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("images/icon.png")));

        primaryStage.setOnCloseRequest((event) -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO);
            alert.setTitle(Constants.APPLICATION_NAME);
            alert.setContentText("Are you sure you want to exit Brainfuck IDE?\n\n");

            DialogPane pane = alert.getDialogPane();
            for (ButtonType t : alert.getButtonTypes()) {
                ((Button) pane.lookupButton(t)).setDefaultButton(t == ButtonType.NO);
            }

            alert.initOwner(primaryStage);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.YES) {
                System.exit(0);
            } else {
                event.consume();
            }
        });

        primaryStage.show();
        primaryStage.setMaximized(true);
        Utils.bringToFront(primaryStage);
    }

    public static void main(String[] args) {
        // create unique instance
        Unique4j unique = new Unique4jList(Constants.APP_ID) {
            @Override
            protected void receiveMessageList(List<String> messageList) {
                // get the array of command line arguments from the list
                String[] stringArgs = messageList.toArray(new String[0]);

                // bring super stage to the front
                Platform.runLater(() -> {
                    Utils.bringToFront(superStage);
                });
            }

            @Override
            protected List<String> sendMessageList() {
                // send the command line arguments as a list
                return Arrays.asList(args);
            }
        };

        // try to obtain lock
        boolean lockFlag = false;
        try {
            lockFlag = unique.acquireLock();
        } catch (Unique4jException e) {
            e.printStackTrace();
        }

        // free lock before JVM exit
        if (lockFlag) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    unique.freeLock();
                } catch (Unique4jException e) {
                    e.printStackTrace();
                }
            }));
        }

        // load fonts before preloader
        loadFonts();

        // launch the application
        launch(args);
    }

}
