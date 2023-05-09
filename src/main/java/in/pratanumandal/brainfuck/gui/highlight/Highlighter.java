package in.pratanumandal.brainfuck.gui.highlight;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import in.pratanumandal.brainfuck.gui.component.TabData;
import javafx.application.Platform;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Highlighter {

    private static final Integer MAX_HIGHLIGHTING_LENGTH = 10000;

    private static final String POINTER_INCREMENT_PATTERN = "\\>";
    private static final String POINTER_DECREMENT_PATTERN = "\\<";
    private static final String DATA_INCREMENT_PATTERN = "\\+";
    private static final String DATA_DECREMENT_PATTERN = "\\-";
    private static final String OUTPUT_PATTERN = "\\.";
    private static final String INPUT_PATTERN = "\\,";
    private static final String LOOP_PATTERN = "\\[|\\]";
    private static final String BREAKPOINT_PATTERN = "\\#";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<PTRINCR>" + POINTER_INCREMENT_PATTERN + ")"
                    + "|(?<PTRDECR>" + POINTER_DECREMENT_PATTERN + ")"
                    + "|(?<DATAINCR>" + DATA_INCREMENT_PATTERN + ")"
                    + "|(?<DATADECR>" + DATA_DECREMENT_PATTERN + ")"
                    + "|(?<OUTPUT>" + OUTPUT_PATTERN + ")"
                    + "|(?<INPUT>" + INPUT_PATTERN + ")"
                    + "|(?<LOOP>" + LOOP_PATTERN + ")"
                    + "|(?<BREAKPOINT>" + BREAKPOINT_PATTERN + ")"
    );

    public static void refreshHighlighting(TabData tabData) {
        if (!Configuration.getSyntaxHighlighting()) return;

        Thread thread = new Thread(() -> {
            int start = 0;
            int length = tabData.getFileText().length();
            int end = start + length;

            doUpdate(tabData, start, end);
        });
        thread.start();
    }

    public static void clearHighlighting(TabData tabData) {
        int start = 0;
        int length = tabData.getFileText().length();
        int end = start + length;

        Utils.runAndWait(() -> tabData.getCodeArea().setStyle(start, end, Collections.singleton("plain-text")));
    }

    private static void doUpdate(TabData tabData, int start, int end) {
        synchronized (tabData) {
            CodeArea codeArea = tabData.getCodeArea();

            if (!tabData.isLargeFile()) {
                String text = codeArea.getText();
                String[] lines = text.split("\r|\n|\r\n");

                int maxLineLength = 0;
                for (String line : lines) {
                    if (line.length() > maxLineLength) maxLineLength = line.length();
                }

                if (maxLineLength >= MAX_HIGHLIGHTING_LENGTH) {
                    tabData.setLargeFile(true);
                }
            }

            if (tabData.isLargeFile()) {
                Platform.runLater(() -> tabData.getBracketHighlighter().highlightBracket());
            }
            else {
                try {
                    String text = codeArea.getText().substring(start, end);
                    StyleSpans<Collection<String>> styleSpans = computeHighlighting(text);

                    Utils.runAndWait(() -> {
                        try {
                            codeArea.setStyleSpans(start, styleSpans);
                        } catch (IndexOutOfBoundsException e) {
                        }
                    });
                    Platform.runLater(() -> tabData.getBracketHighlighter().highlightBracket());
                }
                catch (StringIndexOutOfBoundsException e) { }
            }
        }
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        int lastKwEnd = 0;
        while(matcher.find()) {
            String styleClass =
                    matcher.group("PTRINCR") != null ? "increment-pointer" :
                            matcher.group("PTRDECR") != null ? "decrement-pointer" :
                                    matcher.group("DATAINCR") != null ? "increment-data" :
                                            matcher.group("DATADECR") != null ? "decrement-data" :
                                                    matcher.group("INPUT") != null ? "input" :
                                                            matcher.group("OUTPUT") != null ? "output" :
                                                                    matcher.group("LOOP") != null ? "loop" :
                                                                            matcher.group("BREAKPOINT") != null ? "breakpoint" :
                                                                                    null; /* never happens */ assert styleClass != null;

            spansBuilder.add(Collections.singleton("plain-text"), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.singleton("plain-text"), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

}
