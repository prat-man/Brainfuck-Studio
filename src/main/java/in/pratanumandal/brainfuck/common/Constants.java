package in.pratanumandal.brainfuck.common;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Constants {

    public static final String APPLICATION_NAME = "Brainfuck Studio";

    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;
    public static final String APPLICATION_VERSION = MAJOR_VERSION + "." + MINOR_VERSION;

    public static final String WEBSITE_URL = "https://brainfuck.pratanumandal.in/";
    public static final String AUTHOR_URL = "https://pratanumandal.in/";
    public static final String LICENSE_URL = "https://github.com/prat-man/Brainfuck-Studio/blob/master/LICENSE";
    public static final String RELEASE_URL = "https://raw.githubusercontent.com/prat-man/Brainfuck-Studio/main/release.yaml";

    public static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".brainfuck-studio";

    public static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";

    public static final String SNIPPETS_FILE = CONFIG_DIR + File.separator + "snippets.json";

    public static final String WELCOME_FILE = CONFIG_DIR + File.separator + "welcome.bf";

    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10);

}
