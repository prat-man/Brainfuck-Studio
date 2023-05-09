package in.pratanumandal.brainfuck.common;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Configuration {

    private static Configuration instance;
    private static final List<Integer> FONT_SIZES = Arrays.asList(10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40);
    private static Boolean firstRun = false;

    private Integer cellSize;
    private Integer memorySize;
    private Integer fontSize;
    private Boolean wrapText;
    private Boolean autoComplete;
    private Boolean syntaxHighlighting;
    private Boolean bracketHighlighting;
    private Boolean autoSave;
    private Boolean showTips;
    private String initialDirectory;

    private Configuration() {
        Configurations configs = new Configurations();
        try {
            File file = new File(Constants.CONFIG_FILE);
            if (!file.exists()) {
                firstRun = true;
                file.createNewFile();
            }

            PropertiesConfiguration config = configs.properties(file);

            this.cellSize = config.getInteger("cellSize", 8);
            this.memorySize = config.getInteger("memorySize", 30000);
            this.fontSize = config.getInteger("fontSize", 14);
            this.wrapText = config.getBoolean("wrapText", false);
            this.autoComplete = config.getBoolean("autoComplete", true);
            this.syntaxHighlighting = config.getBoolean("syntaxHighlighting", true);
            this.bracketHighlighting = config.getBoolean("bracketHighlighting", true);
            this.autoSave = config.getBoolean("autoSave", true);
            this.showTips = config.getBoolean("showTips", true);
            this.initialDirectory = config.getString("initialDirectory", System.getProperty("user.home"));
        }
        catch (ConfigurationException | IOException e) {
            e.printStackTrace();

            this.cellSize = 8;
            this.memorySize = 30000;
            this.fontSize = 14;
            this.wrapText = false;
            this.autoComplete = true;
            this.syntaxHighlighting = true;
            this.bracketHighlighting = true;
            this.autoSave = true;
            this.showTips = true;
            this.initialDirectory = System.getProperty("user.home");
        }
    }

    private static void sanitize() {
        boolean exists = true;
        if (instance == null) {
            instance = new Configuration();
            exists = false;
        }

        if (instance.cellSize == null || (instance.cellSize != 8 && instance.cellSize != 16)) instance.cellSize = 8;
        if (instance.memorySize == null || (instance.memorySize < 1000 || instance.memorySize > 50000)) instance.memorySize = 30000;
        if (instance.fontSize == null || (!FONT_SIZES.contains(instance.fontSize))) instance.fontSize = 14;
        if (instance.wrapText == null) instance.wrapText = false;
        if (instance.autoComplete == null) instance.autoComplete = true;
        if (instance.syntaxHighlighting == null) instance.syntaxHighlighting = true;
        if (instance.bracketHighlighting == null) instance.bracketHighlighting = true;
        if (instance.autoSave == null) instance.autoSave = true;
        if (instance.showTips == null) instance.showTips = true;
        if (instance.initialDirectory == null || !Files.isDirectory(Path.of(instance.initialDirectory))) instance.initialDirectory = System.getProperty("user.home");

        if (!exists) {
            try {
                flush(false);
            } catch (ConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Integer getCellSize() {
        sanitize();
        return instance.cellSize;
    }

    public static Integer getMemorySize() {
        sanitize();
        return instance.memorySize;
    }

    public static Integer getFontSize() {
        sanitize();
        return instance.fontSize;
    }

    public static Boolean getWrapText() {
        sanitize();
        return instance.wrapText;
    }

    public static Boolean getAutoComplete() {
        sanitize();
        return instance.autoComplete;
    }

    public static Boolean getSyntaxHighlighting() {
        sanitize();
        return instance.syntaxHighlighting;
    }

    public static Boolean getBracketHighlighting() {
        sanitize();
        return instance.bracketHighlighting;
    }

    public static Boolean getAutoSave() {
        sanitize();
        return instance.autoSave;
    }

    public static Boolean getShowTips() {
        sanitize();
        return instance.showTips;
    }

    public static File getInitialDirectory() {
        sanitize();
        return new File(instance.initialDirectory);
    }

    public static void setCellSize(Integer cellSize) {
        sanitize();
        instance.cellSize = cellSize;
    }

    public static void setMemorySize(Integer memorySize) {
        sanitize();
        instance.memorySize = memorySize;
    }

    public static void setFontSize(Integer fontSize) {
        sanitize();
        instance.fontSize = fontSize;
    }

    public static void setWrapText(Boolean wrapText) {
        sanitize();
        instance.wrapText = wrapText;
    }

    public static void setAutoComplete(Boolean autoComplete) {
        sanitize();
        instance.autoComplete = autoComplete;
    }

    public static void setSyntaxHighlighting(Boolean syntaxHighlighting) {
        sanitize();
        instance.syntaxHighlighting = syntaxHighlighting;
    }

    public static void setBracketHighlighting(Boolean bracketHighlighting) {
        sanitize();
        instance.bracketHighlighting = bracketHighlighting;
    }

    public static void setAutoSave(Boolean autoSave) {
        sanitize();
        instance.autoSave = autoSave;
    }

    public static void setShowTips(Boolean showTips) {
        sanitize();
        instance.showTips = showTips;
    }

    public static void setInitialDirectory(File initialDirectory) {
        sanitize();
        instance.initialDirectory = initialDirectory.getAbsolutePath();
    }

    public static Boolean isFirstRun() {
        return firstRun;
    }

    public static void flush() throws ConfigurationException, IOException {
        flush(true);
    }

    private static void flush(boolean sanitize) throws ConfigurationException, IOException {
        if (sanitize) sanitize();

        File file = new File(Constants.CONFIG_FILE);
        if (file.exists()) file.delete();
        file.createNewFile();

        Configurations configs = new Configurations();
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = configs.propertiesBuilder(file);
        PropertiesConfiguration config = builder.getConfiguration();

        config.addProperty("cellSize", instance.cellSize);
        config.addProperty("memorySize", instance.memorySize);
        config.addProperty("fontSize", instance.fontSize);
        config.addProperty("wrapText", instance.wrapText);
        config.addProperty("autoComplete", instance.autoComplete);
        config.addProperty("syntaxHighlighting", instance.syntaxHighlighting);
        config.addProperty("bracketHighlighting", instance.bracketHighlighting);
        config.addProperty("autoSave", instance.autoSave);
        config.addProperty("showTips", instance.showTips);
        config.addProperty("initialDirectory", instance.initialDirectory);

        builder.save();
    }

}
