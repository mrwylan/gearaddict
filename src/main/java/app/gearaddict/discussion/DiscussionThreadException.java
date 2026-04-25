package app.gearaddict.discussion;

public class DiscussionThreadException extends RuntimeException {

    public enum Reason {
        TITLE_REQUIRED,
        TITLE_TOO_SHORT,
        TITLE_TOO_LONG,
        BODY_REQUIRED,
        BODY_TOO_SHORT,
        BODY_TOO_LONG,
        EQUIPMENT_NOT_FOUND,
        AUTHOR_NOT_FOUND
    }

    private final Reason reason;

    public DiscussionThreadException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
