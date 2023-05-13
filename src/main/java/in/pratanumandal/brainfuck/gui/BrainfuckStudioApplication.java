package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.controller.Controller;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BrainfuckStudioApplication extends Application {

    public static HostServices hostServices;

    private static void loadFonts() {
        Font.loadFont(BrainfuckStudioApplication.class.getClassLoader().getResourceAsStream("fonts/OpenSans-Regular.ttf"), 12);
        Font.loadFont(BrainfuckStudioApplication.class.getClassLoader().getResourceAsStream("fonts/OpenSans-Bold.ttf"), 12);
        Font.loadFont(BrainfuckStudioApplication.class.getClassLoader().getResourceAsStream("fonts/VeraMono.ttf"), 12);
    }

    @Override
    public void init() throws Exception {
        super.init();

        Utils.setTaskbarIcon();

        hostServices = getHostServices();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/main.fxml"));
        Parent root = loader.load();

        Controller controller = loader.getController();
        controller.setStage(primaryStage);

        Scene scene = new Scene(root, 780, 500);

        primaryStage.setTitle(Constants.APPLICATION_NAME);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(550);
        primaryStage.setMinHeight(350);
        primaryStage.sizeToScene();

        Utils.setStageIcon(primaryStage);
        Utils.setStylesheet(primaryStage);
        Utils.setStyle(primaryStage);

        primaryStage.setOnCloseRequest((event) -> {
            if (controller.exitApplication()) {
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
        // load fonts before preloader
        loadFonts();

        // create config directory if not exists
        try {
            Files.createDirectories(Path.of(Constants.CONFIG_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // launch the application
        launch(args);
    }

}
