package in.pratanumandal.brainfuck.engine.processor.translator;

import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.engine.processor.Processor;
import in.pratanumandal.brainfuck.gui.NotificationManager;
import in.pratanumandal.brainfuck.gui.TabData;
import javafx.application.Platform;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Translator extends Processor {

    protected File outputFile;

    public Translator(TabData tabData) {
        super(tabData);
    }

    public void start() {
        String outputFilePath = tabData.getFilePath().substring(0, tabData.getFilePath().length() - 2) + this.getExtension();
        this.outputFile = new File(outputFilePath);
        super.start();
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

            Platform.runLater(() -> Utils.addNotification("File " + tabData.getTab().getText() + " export failed"));
            this.stop(false);
            return;
        }

        Platform.runLater(() -> notification.close());

        if (this.kill.get()) {
            Platform.runLater(() -> Utils.addNotification("File " + tabData.getTab().getText() + " export terminated"));
        }
        else {
            Platform.runLater(() -> Utils.addNotification("File " + tabData.getTab().getText() + " exported to " + this.getLanguage()));
        }

        this.stop(false);

    }

    public String getFileNameWithoutExtension() {
        return outputFile.getName().substring(0, outputFile.getName().length() - this.getExtension().length() - 1);
    }

    public abstract void doTranslate(NotificationManager.Notification notification, BufferedWriter writer) throws IOException;

    public abstract String getLanguage();

    public abstract String getExtension();

}
