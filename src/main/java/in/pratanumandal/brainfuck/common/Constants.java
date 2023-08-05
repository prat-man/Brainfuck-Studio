package in.pratanumandal.brainfuck.common;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Constants {

    public static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".brainfuck-studio";

    public static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";

    public static final String SNIPPETS_FILE = CONFIG_DIR + File.separator + "snippets.json";

    public static final String WELCOME_FILE = CONFIG_DIR + File.separator + "welcome.bf";

    public static final String APPLICATION_NAME = "Brainfuck Studio";

    public static final String APPLICATION_VERSION = "1.0";

    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10);

}
