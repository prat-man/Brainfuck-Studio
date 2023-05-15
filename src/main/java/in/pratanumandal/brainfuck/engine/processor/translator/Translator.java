package in.pratanumandal.brainfuck.engine.processor.translator;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Constants;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.UnmatchedBracketException;
import in.pratanumandal.brainfuck.engine.processor.Processor;
import in.pratanumandal.brainfuck.gui.component.NotificationManager;
import in.pratanumandal.brainfuck.gui.component.TabData;
import in.pratanumandal.brainfuck.os.windows.WindowsUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Translator extends Processor {

    protected File outputFile;

    protected Integer cellSize;

    public Translator(TabData tabData) {
        super(tabData);
    }

    public void start() {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(Constants.APPLICATION_NAME);

        if (tabData.getFilePath() != null) {
            String outputFilePath = tabData.getFilePath().substring(0, tabData.getFilePath().length() - 2) + this.getExtension();
            this.outputFile = new File(outputFilePath);

            fileChooser.setInitialDirectory(this.outputFile.getParentFile());
            fileChooser.setInitialFileName(this.outputFile.getName());
        }
        else {
            fileChooser.setInitialDirectory(Configuration.getInitialDirectory());
        }

        fileChooser.getExtensionFilters().add(this.getExtensionFilter());

        File file = fileChooser.showSaveDialog(tabData.getTab().getTabPane().getScene().getWindow());

        if (file != null) {
            Configuration.setInitialDirectory(file.getParentFile());
            try {
                Configuration.flush();
            } catch (ConfigurationException | IOException e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Failed to save configuration!");
                WindowsUtils.setStageStyle((Stage) error.getDialogPane().getScene().getWindow());
                error.initOwner(tabData.getTab().getTabPane().getScene().getWindow());
                error.showAndWait();
            }

            this.outputFile = file;

            this.cellSize = Configuration.getCellSize();

            try {
                super.start();
            } catch (UnmatchedBracketException e) {
                Platform.runLater(() -> {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle(Constants.APPLICATION_NAME);
                    error.setHeaderText("Translator Error");
                    error.setContentText(e.getMessage() + "\n\n");

                    WindowsUtils.setStageStyle((Stage) error.getDialogPane().getScene().getWindow());

                    error.initOwner(tabData.getTab().getTabPane().getScene().getWindow());
                    error.showAndWait();
                });
            }
        }
    }

    @Override
    public void run() {
        AtomicReference<NotificationManager.Notification> notificationAtomicReference = new AtomicReference<>();
        Utils.runAndWait(() -> notificationAtomicReference.set(Utils.addNotificationWithProgress("Exporting file " + tabData.getTab().getText() + " to " + this.getLanguage())));

        NotificationManager.Notification notification = notificationAtomicReference.get();

        notification.addListener(() -> this.stop(false));

        try (
                BufferedWriter bw = new BufferedWriter(new FileWriter(this.outputFile));
        ) {
            this.doTranslate(notification, bw);
        } catch (IOException e) {
            e.printStackTrace();

            Platform.runLater(() -> Utils.addNotification(tabData.getTab().getText() + " export failed"));
            this.stop(false);
            return;
        }

        Platform.runLater(() -> notification.close());

        if (this.kill.get()) {
            Platform.runLater(() -> Utils.addNotification(tabData.getTab().getText() + " export terminated"));
        }
        else {
            Platform.runLater(() -> Utils.addNotification(tabData.getTab().getText() + " exported to " + this.getLanguage()));
        }

        this.stop(false);
    }

    public String getFileNameWithoutExtension() {
        return outputFile.getName().substring(0, outputFile.getName().length() - this.getExtension().length() - 1);
    }

    public abstract void doTranslate(NotificationManager.Notification notification, BufferedWriter writer) throws IOException;

    public abstract String getLanguage();

    public abstract String getExtension();

    public abstract FileChooser.ExtensionFilter getExtensionFilter();

}
