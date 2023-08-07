package in.pratanumandal.brainfuck.common;

public class CharacterUtils {

    public static Long getCodePoint(Byte value) {
        return Byte.toUnsignedLong(value);
    }

    public static Long getCodePoint(Short value) {
        return Short.toUnsignedLong(value);
    }

    public static Long getCodePoint(Integer value) {
        return Integer.toUnsignedLong(value);
    }

    public static String getSymbol(Byte value) {
        return getSymbol(getCodePoint(value));
    }

    public static String getSymbol(Short value) {
        return getSymbol(getCodePoint(value));
    }

    public static String getSymbol(Integer value) {
        return getSymbol(getCodePoint(value));
    }

    public static String getSymbol(Long codePoint) {
        try {
            return String.valueOf(Character.toChars(codePoint.intValue()));
        }
        catch (IllegalArgumentException e) {
            return "N/A";
        }
    }

}
