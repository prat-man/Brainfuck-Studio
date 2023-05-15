package in.pratanumandal.brainfuck.os.windows;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class WindowsUtils {

    public static void setStageStyle(Stage stage) {
        Platform.runLater(() -> {
            StageOps.WindowHandle handle = StageOps.findWindowHandle(stage);
            StageOps.setCaptionColor(handle, Color.rgb(78, 83, 92));
        });
    }

}
