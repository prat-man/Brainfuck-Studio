package in.pratanumandal.brainfuck.common;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;
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

    public static final String APPLICATION_NAME = "Brainfuck IDE";

    public static final String APPLICATION_VERSION = "1.0";

    public static final String INTERPRETER_COMPILER_VERSION = "1.2";

    public static final AtomicReference<File> BROWSE_DIRECTORY = new AtomicReference<>(new File(System.getProperty("user.home")));

    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10);

    public static final Integer MEMORY_SIZE = 30000;

    public static final Integer CELL_SIZE = 16;


    private static File executable;

    public static String getExecutablePath() {
        return getExecutableFile().getAbsolutePath();
    }

    public static File getExecutableFile() {
        if (executable != null && executable.exists() && executable.isFile()) return executable;
        executable = generateExecutableFile();
        return executable;
    }

    private static File generateExecutableFile() {
        try {
            InputStream inputStream = null;
            Path path = null;

            if (SystemUtils.IS_OS_WINDOWS) {
                inputStream = Constants.class.getClassLoader().getResourceAsStream("bin/brainfuck.windows");
                path = getUniqueFilePath("exe");
            }
            else if (SystemUtils.IS_OS_LINUX) {
                inputStream = Constants.class.getClassLoader().getResourceAsStream("bin/brainfuck.linux");
                path = getUniqueFilePath();
            }

            Files.copy(inputStream, path);

            File file = path.toFile();
            file.setExecutable(true);
            file.deleteOnExit();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Path getUniqueFilePath() {
        return getUniqueFilePath(null);
    }

    private static Path getUniqueFilePath(String extension) {
        Date date = new Date();

        if (extension == null || extension.isEmpty()) {
            return new File(System.getProperty("java.io.tmpdir"), new StringBuilder().append("brainfuck-")
                    .append(date.getTime()).append(UUID.randomUUID()).toString()).toPath();
        }
        else {
            return new File(System.getProperty("java.io.tmpdir"), new StringBuilder().append("brainfuck-")
                    .append(date.getTime()).append(UUID.randomUUID())
                    .append(".").append(extension).toString()).toPath();
        }
    }

}
