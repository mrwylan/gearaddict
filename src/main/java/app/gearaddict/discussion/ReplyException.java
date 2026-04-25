package app.gearaddict.discussion;

public class ReplyException extends RuntimeException {

    public enum Reason {
        BODY_REQUIRED,
        BODY_TOO_LONG,
        THREAD_NOT_FOUND,
        AUTHOR_NOT_FOUND
    }

    private final Reason reason;

    public ReplyException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
