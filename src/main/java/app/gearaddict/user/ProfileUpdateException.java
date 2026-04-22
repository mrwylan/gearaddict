package app.gearaddict.user;

public class ProfileUpdateException extends RuntimeException {

    public enum Reason {
        USERNAME_EMPTY,
        USERNAME_TOO_LONG,
        USERNAME_TAKEN,
        BIO_TOO_LONG,
        USER_NOT_FOUND
    }

    private final Reason reason;

    public ProfileUpdateException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
