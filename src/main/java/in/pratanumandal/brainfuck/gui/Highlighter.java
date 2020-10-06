package in.pratanumandal.brainfuck.gui;

import in.pratanumandal.brainfuck.common.Configuration;
import in.pratanumandal.brainfuck.common.Utils;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Highlighter {

    private static final String POINTER_INCREMENT_PATTERN = "\\>";
    private static final String POINTER_DECREMENT_PATTERN = "\\<";
    private static final String DATA_INCREMENT_PATTERN = "\\+";
    private static final String DATA_DECREMENT_PATTERN = "\\-";
    private static final String OUTPUT_PATTERN = "\\.";
    private static final String INPUT_PATTERN = "\\,";
    private static final String LOOP_PATTERN = "\\[|\\]";
    private static final String BREAKPOINT_PATTERN = "\\~";

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

    public static void computeHighlighting(List<PlainTextChange> changes, TabData tabData) {
        if (!Configuration.getSyntaxHighlighting()) return;

        Thread thread = new Thread(() -> {
            for (PlainTextChange change : changes) {
                int start = change.getPosition();
                int length = change.getInserted().replace("\r\n", "\n").length();
                int end = start + length;

                doUpdate(tabData, start, end);
            }
        });
        thread.start();
    }

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
        CodeArea codeArea = tabData.getCodeArea();

        String text = codeArea.getText().substring(start, end);
        int chunkSize = 10000;
        String[] splitText = IntStream
                .iterate(0, i -> i + chunkSize)
                .limit((int) Math.ceil(text.length() / (double) chunkSize))
                .mapToObj(i -> text.substring(i, Math.min(i + chunkSize, text.length())))
                .toArray(String[]::new);

        if (splitText.length >= 10) {
            tabData.setLargeFile(true);
        }

        if (tabData.isLargeFile()) {
            Platform.runLater(() -> tabData.getBracketHighlighter().highlightBracket());
            return;
        }
        else if (splitText.length >= 5) {
            Tab tab = tabData.getTab();

            ProgressBar progressBar = new ProgressBar();
            progressBar.setProgress(0);

            StackPane stackPane = new StackPane();
            stackPane.getChildren().add(progressBar);
            stackPane.getStyleClass().add("dark-tab-background");

            Platform.runLater(() -> tab.setContent(stackPane));

            for (int i = 0; i < splitText.length; i++) {
                StyleSpans<Collection<String>> styleSpans = computeHighlighting(splitText[i]);
                int from = i * chunkSize;

                Platform.runLater(() -> {
                    try {
                        codeArea.setStyleSpans(from + start, styleSpans);
                    } catch (IndexOutOfBoundsException e) {
                    }
                    tabData.getBracketHighlighter().highlightBracket();
                });

                double progress = i / (double) splitText.length;
                progressBar.setProgress(progress);

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) { }
            }

            Platform.runLater(() -> tab.setContent(tabData.getSplitPane()));
        }
        else {
            for (int i = 0; i < splitText.length; i++) {
                StyleSpans<Collection<String>> styleSpans = computeHighlighting(splitText[i]);
                int from = i * chunkSize;

                Platform.runLater(() -> {
                    try {
                        codeArea.setStyleSpans(from + start, styleSpans);
                    } catch (IndexOutOfBoundsException e) {
                    }
                    tabData.getBracketHighlighter().highlightBracket();
                });

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) { }
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

            //spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton("plain-text"), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        //spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        spansBuilder.add(Collections.singleton("plain-text"), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

}
