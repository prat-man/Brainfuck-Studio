package in.pratanumandal.brainfuck.common;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Configuration {

    public static final String CONFIG_FILE = "config.properties";

    private static Configuration instance;
    private static final List<Integer> FONT_SIZES = Arrays.asList(new Integer[] {10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40});
    private static Boolean firstRun = false;

    private Integer cellSize;
    private Integer memorySize;
    private Integer fontSize;
    private Boolean wrapText;
    private Boolean showTips;

    private Configuration() {
        Configurations configs = new Configurations();
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                firstRun = true;
                file.createNewFile();
            }

            PropertiesConfiguration config = configs.properties(file);

            this.cellSize = config.getInteger("cellSize", 8);
            this.memorySize = config.getInteger("memorySize", 30000);
            this.fontSize = config.getInteger("fontSize", 16);
            this.wrapText = config.getBoolean("wrapText", false);
            this.showTips = config.getBoolean("showTips", true);
        }
        catch (ConfigurationException | IOException e) {
            e.printStackTrace();

            this.cellSize = 8;
            this.memorySize = 30000;
            this.fontSize = 16;
            this.wrapText = false;
            this.showTips = true;
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
        if (instance.fontSize == null || (!FONT_SIZES.contains(instance.fontSize))) instance.fontSize = 16;
        if (instance.wrapText == null) instance.wrapText = false;
        if (instance.showTips == null) instance.showTips = true;

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

    public static Boolean getShowTips() {
        sanitize();
        return instance.showTips;
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

    public static void setShowTips(Boolean showTips) {
        sanitize();
        instance.showTips = showTips;
    }

    public static Boolean isFirstRun() {
        return firstRun;
    }

    public static void flush() throws ConfigurationException, IOException {
        flush(true);
    }

    private static void flush(boolean sanitize) throws ConfigurationException, IOException {
        if (sanitize) sanitize();

        File file = new File(CONFIG_FILE);
        if (file.exists()) file.delete();
        file.createNewFile();

        Configurations configs = new Configurations();
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = configs.propertiesBuilder(file);
        PropertiesConfiguration config = builder.getConfiguration();

        config.addProperty("cellSize", instance.cellSize);
        config.addProperty("memorySize", instance.memorySize);
        config.addProperty("fontSize", instance.fontSize);
        config.addProperty("wrapText", instance.wrapText);
        config.addProperty("showTips", instance.showTips);

        builder.save();
    }

}
