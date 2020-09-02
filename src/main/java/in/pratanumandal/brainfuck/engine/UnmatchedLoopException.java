package in.pratanumandal.brainfuck.engine;

public class UnmatchedLoopException extends RuntimeException {

    public UnmatchedLoopException() {
    }

    public UnmatchedLoopException(String message) {
        super(message);
    }

    public UnmatchedLoopException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnmatchedLoopException(Throwable cause) {
        super(cause);
    }

    public UnmatchedLoopException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
