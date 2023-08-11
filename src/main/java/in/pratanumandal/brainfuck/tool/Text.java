package in.pratanumandal.brainfuck.tool;

import java.util.ArrayList;
import java.util.List;

public class Text {

    private static List<Integer> computeDelta(String text, boolean optimize) {
        int[] codePoints = text.codePoints().toArray();
        List<Integer> delta = new ArrayList<>();

        delta.add(codePoints[0]);
        for (int i = 1; i < codePoints.length; i++) {
            int diff = codePoints[i] - codePoints[i - 1];
            if (optimize && Math.abs(diff) > codePoints[i]) {
                delta.add(null);
                delta.add(codePoints[i]);
            }
            else {
                delta.add(diff);
            }
        }

        return delta;
    }

    public static String convertToBrainfuck(String text, boolean optimize) {
        List<Integer> delta = computeDelta(text, optimize);

        StringBuilder sb = new StringBuilder();
        for (Integer n : delta) {
            if (n == null) {
                sb.append(">");
            }
            else {
                sb.append(Number.convertToBrainfuck(n));
                sb.append(".");
            }
        }

        return sb.toString();
    }

    public static String convertToBrainfuck(String text) {
        return convertToBrainfuck(text, true);
    }

}
