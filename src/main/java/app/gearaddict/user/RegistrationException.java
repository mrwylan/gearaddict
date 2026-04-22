package app.gearaddict.user;

public class RegistrationException extends RuntimeException {

    public enum Reason {
        USERNAME_TAKEN,
        EMAIL_TAKEN,
        PASSWORD_TOO_SHORT,
        MISSING_FIELD,
        INVALID_EMAIL
    }

    private final Reason reason;

    public RegistrationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
