package in.pratanumandal.brainfuck.engine;

public class UnmatchedBracketException extends RuntimeException {

    public UnmatchedBracketException() {
    }

    public UnmatchedBracketException(String message) {
        super(message);
    }

    public UnmatchedBracketException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnmatchedBracketException(Throwable cause) {
        super(cause);
    }

    public UnmatchedBracketException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
