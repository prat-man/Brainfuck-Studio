package in.pratanumandal.brainfuck.common;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class Constants {

    public static final String BASIC_INFO = String.join("\n", new String[] {
            "> \tincrement the data pointer (to point to the next cell to the right).\n" +
                    "< \tdecrement the data pointer (to point to the next cell to the left).\n" +
                    "+ \tincrement (increase by one) the byte at the data pointer.\n" +
                    "- \tdecrement (decrease by one) the byte at the data pointer.\n" +
                    ". \toutput the byte at the data pointer.\n" +
                    ", \taccept one byte of input, storing its value in the byte at the data pointer.\n" +
                    "[ \tif the byte at the data pointer is zero, then instead of moving the instruction pointer forward to the next command, jump it forward to the command after the matching ] command.\n" +
                    "] \tif the byte at the data pointer is nonzero, then instead of moving the instruction pointer forward to the next command, jump it back to the command after the matching [ command.\n"
    });

    // unique application ID
    public static final String APP_ID = "in.pratanumandal.brainguck-hQnxoK-20201003-#z.9";

    public static final String APPLICATION_NAME = "Brainfuck IDE";

    public static final String APPLICATION_VERSION = "1.0";

    public static final String INTERPRETER_COMPILER_VERSION = "1.2";

    public static final AtomicReference<File> BROWSE_DIRECTORY = new AtomicReference<>(new File(System.getProperty("user.home")));

    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10);

}
