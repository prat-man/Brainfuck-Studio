package in.pratanumandal.brainfuck.tool;

import in.pratanumandal.brainfuck.common.Utils;

import java.util.ArrayList;
import java.util.List;

public class Text {

    private static List<Integer> computeDelta(String text, boolean optimize) {
        char[] textArray = text.toCharArray();
        List<Integer> delta = new ArrayList<>();

        delta.add((int) textArray[0]);
        for (int i = 1; i < textArray.length; i++) {
            int diff = textArray[i] - textArray[i - 1];
            if (optimize && Math.abs(diff) > textArray[i]) {
                delta.add(null);
                delta.add((int) textArray[i]);
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
            }
        }

        return sb.toString();
    }

    public static String convertToBrainfuck(String text) {
        return convertToBrainfuck(text, true);
    }

    public static void main(String[] args) {
        String text = "Hello World!";

        String converted = convertToBrainfuck(text);
        converted = Utils.formatBrainfuck(converted);
        System.out.println(converted);
    }

}
