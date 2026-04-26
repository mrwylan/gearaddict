package app.gearaddict.equipment;

public class CatalogException extends RuntimeException {

    public enum Reason {
        NAME_REQUIRED,
        NAME_TOO_LONG,
        NAME_TOO_SHORT,
        MANUFACTURER_REQUIRED,
        CATEGORY_REQUIRED,
        DUPLICATE_EQUIPMENT,
        DUPLICATE_MANUFACTURER,
        DESCRIPTION_TOO_LONG,
        MANUFACTURER_IN_USE,
        NOT_FOUND
    }

    private final Reason reason;
    private final Long conflictingId;

    public CatalogException(Reason reason, String message) {
        this(reason, message, null);
    }

    public CatalogException(Reason reason, String message, Long conflictingId) {
        super(message);
        this.reason = reason;
        this.conflictingId = conflictingId;
    }

    public Reason reason() {
        return reason;
    }

    public Long conflictingId() {
        return conflictingId;
    }
}
