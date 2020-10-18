package in.pratanumandal.brainfuck.common;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class Constants {

    public static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".brainfuck-ide";

    public static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";

    public static final String WELCOME_FILE = CONFIG_DIR + File.separator + "welcome.bf";

    public static final String WELCOME_TEXT = "WELCOME TO BRAINFUCK IDE\n" +
            "========================\n" +
            "\n" +
            "[[\n" +
            "\n" +
            "BRAINFUCK OPERATORS\n" +
            "\n" +
            "    >    increment the data pointer (to point to the next cell to the right).\n" +
            "    <    decrement the data pointer (to point to the next cell to the left).\n" +
            "    +    increment (increase by one) the byte at the data pointer.\n" +
            "    -    decrement (decrease by one) the byte at the data pointer.\n" +
            "    .    output the byte at the data pointer.\n" +
            "    ,    accept one byte of input, storing its value in the byte at the data pointer.\n" +
            "    [    if the byte at the data pointer is zero, then instead of moving the instruction pointer forward to the next command, jump it forward to the command after the matching ] command.\n" +
            "    ]    if the byte at the data pointer is nonzero, then instead of moving the instruction pointer forward to the next command, jump it back to the command after the matching [ command.\n" +
            "\n" +
            "\n" +
            "IDE SPECIFIC OPERATORS\n" +
            "\n" +
            "    ~    breakpoint - while debugging pauses the execution when encountered. this is a non-standard feature specific to Brainfuck IDE.\n" +
            "\n" +
            "]]\n" +
            "\n" +
            "\n" +
            "+++++++++[->+++++++++<]>++++++.<+++[->+++<]>+++++.+++++++.--\n" +
            "-------.<+++[->+++<]>+++.--.--------.<++++++++[->--------<]>\n" +
            "-----.<+++++++++[->+++++++++<]>+++.-----.<++++++++[->-------\n" +
            "-<]>---------------.<+++++[->+++++<]>+++++++++.<++++++[->+++\n" +
            "+++<]>++++++++++++.<++++[->----<]>-.++++++++.+++++.--------.\n" +
            "<+++[->+++<]>++++++.<++++[->----<]>--.++++++++.<++++++++[->-\n" +
            "-------<]>-----------.<++++++[->++++++<]>+++++.-----.+.<++++\n" +
            "[->----<]>-------.<+++++[->-----<]>--------.---.+++.---.<+++\n" +
            "++++[->+++++++<]>+++++++++++++.<++++++[->++++++<]>+++.+.<+++\n" +
            "[->---<]>--.<++++++++[->--------<]>-----.<+++++++++[->++++++\n" +
            "+++<]>++++++++.<+++[->---<]>-.++++++.<+++++++++[->---------<\n" +
            "]>----.<++++++++[->++++++++<]>+++++.+++++++++.----.+++++.<++\n" +
            "+[->+++<]>+.<+++++++++[->---------<]>--------.<+++++++++[->+\n" +
            "++++++++<]>++++.--.<+++[->---<]>-.+++++.-------.<++++++++[->\n" +
            "--------<]>-------.<++++++++[->++++++++<]>+++++++++.<+++[->+\n" +
            "++<]>++.<++++++++[->--------<]>------.<+++[->---<]>-----.<++\n" +
            "+++[->+++++<]>++++++++++.<++++++[->++++++<]>+.---..<+++[->++\n" +
            "+<]>++++.+.<+++++++++[->---------<]>-.<++++[->----<]>----.--\n" +
            "-.+++.---.<++++[->++++<]>++++++................<+++[->+++<]>\n" +
            "++++.<+++[->---<]>----.<++++++[->++++++<]>++++++++++++.<++++\n" +
            "+[->+++++<]>+++++++++.<++++[->----<]>-.<++++[->++++<]>+++.<+\n" +
            "+++[->----<]>---.<+++[->+++<]>++++.+++++++.<+++++++++[->----\n" +
            "-----<]>----.<++++++[->++++++<]>+++++++++.<++++[->++++<]>+++\n" +
            "+.<+++[->+++<]>++++.<+++[->---<]>-.---.<+++[->+++<]>++.<\n";

    public static final String APPLICATION_NAME = "Brainfuck IDE";

    public static final String APPLICATION_VERSION = "1.0";

    public static final AtomicReference<File> BROWSE_DIRECTORY = new AtomicReference<>(new File(System.getProperty("user.home")));

    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10);

}
