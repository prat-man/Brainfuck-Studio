package in.pratanumandal.brainfuck;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.event.EventHandler;
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
import javafx.stage.WindowEvent;

public class Main extends Application {

    public static HostServices hostServices;

    @Override
    public void init() throws Exception {
        super.init();

        hostServices = getHostServices();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("fonts/OpenSans-Regular.ttf"), 12);
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("fonts/VeraMono.ttf"), 14);

        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/main.fxml"));
        Parent root = loader.load();

        Controller controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.setTitle(Constants.APPLICATION_NAME);
        primaryStage.setScene(new Scene(root, 780, 500));
        primaryStage.setMinWidth(550);
        primaryStage.setMinHeight(350);
        primaryStage.sizeToScene();
        primaryStage.show();
        primaryStage.setMaximized(true);

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
    }

    public static void main(String[] args) {
        launch(args);
    }

}
