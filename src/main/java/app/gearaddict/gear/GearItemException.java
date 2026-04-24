package app.gearaddict.gear;

public class GearItemException extends RuntimeException {

    public enum Reason {
        CATEGORY_REQUIRED,
        EQUIPMENT_OR_NAME_REQUIRED,
        NAME_TOO_LONG,
        NOTES_TOO_LONG,
        NOT_FOUND,
        NOT_OWNER
    }

    private final Reason reason;

    public GearItemException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
