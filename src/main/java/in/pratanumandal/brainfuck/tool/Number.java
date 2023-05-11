package in.pratanumandal.brainfuck.tool;

import in.pratanumandal.brainfuck.common.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class Number {

    private static List<Integer> getPrimeFactors(int n) {
        if (n < 0) throw new IllegalArgumentException("Only positive numbers allowed");
        if (n == 0) return Collections.emptyList();
        if (n == 1) return Collections.singletonList(n);

        List<Integer> factors = new ArrayList<>();

        for (int i = 2; i <= n; i++) {
            while (n % i == 0) {
                factors.add(i);
                n /= i;
            }
        }

        return factors;
    }

    private static List<Integer> getFactors(int n) {
        List<Integer> factors = getPrimeFactors(n);

        while (factors.size() >= 2) {
            int factor0 = factors.get(0);
            int factor1 = factors.get(1);

            double ratio = (factor0 * factor1) / (factor0 + factor1 + 7.0);

            if (ratio > 1) break;

            factors = new ArrayList<>(factors.subList(2, factors.size()));
            factors.add(factor0 * factor1);

            Collections.sort(factors);
        }

        return factors;
    }

    private static String convertToBrainfuck(int n, int sign, int adjustment) {
        String increment = (sign > 0) ? "+" : "-";
        String decrement = (sign > 0) ? "-" : "+";

        List<Integer> factors = getFactors(n);
        StringBuilder sb = new StringBuilder();

        if (factors.size() > 0) {
            sb.append(">".repeat(factors.size() - 1));
        }

        Iterator<Integer> iterator = factors.iterator();
        while (iterator.hasNext()) {
            int count = iterator.next();

            if (iterator.hasNext()) {
                sb.append("+".repeat(count));
                sb.append("[<");
            }
            else {
                sb.append(increment.repeat(count));
            }
        }

        if (factors.size() > 0) {
            sb.append(">-]".repeat(factors.size() - 1));
            sb.append("<".repeat(factors.size() - 1));
        }

        if (adjustment > 0) {
            sb.append(increment.repeat(adjustment));
        }
        else if (adjustment < 0) {
            sb.append(decrement.repeat(-adjustment));
        }

        sb.append(".");

        return sb.toString();
    }

    public static String convertToBrainfuck(int n) {
        // separate sign from number
        int sign = (n < 0) ? -1 : 1;
        n = n * sign;

        // initialize variables
        int x;
        List<String> strings = new ArrayList<>();

        // original number
        strings.add(convertToBrainfuck(n, sign, 0));

        // largest multiple of 10 smaller than the number
        x = (n / 10) * 10;
        strings.add(convertToBrainfuck(x, sign, n - x));

        // smallest multiple of 10 larger than the number
        x = (n / 10 + 1) * 10;
        strings.add(convertToBrainfuck(x, sign, n - x));

        // nearest whole square to the number
        x = (int) Math.round(Math.sqrt(n));
        x = x * x;
        strings.add(convertToBrainfuck(x, sign, n - x));

        // return string with minimum length
        return strings.stream().min(Comparator.comparingInt(String::length)).get();
    }

    public static void main(String[] args) {
        int n = 32;

        String converted = convertToBrainfuck(n);
        converted = Utils.formatBrainfuck(converted);
        System.out.println(converted);
    }

}
