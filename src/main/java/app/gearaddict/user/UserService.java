package app.gearaddict.user;

import app.gearaddict.jooq.tables.records.UsersRecord;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    public static final int BCRYPT_COST = 12;
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String email = request.email() == null ? "" : request.email().trim();
        String password = request.password() == null ? "" : request.password();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            throw new RegistrationException(
                    RegistrationException.Reason.MISSING_FIELD,
                    "Username, email, and password are required.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RegistrationException(
                    RegistrationException.Reason.INVALID_EMAIL,
                    "Email address is not valid.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new RegistrationException(
                    RegistrationException.Reason.PASSWORD_TOO_SHORT,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new RegistrationException(
                    RegistrationException.Reason.USERNAME_TAKEN,
                    "Username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException(
                    RegistrationException.Reason.EMAIL_TAKEN,
                    "Email is already registered.");
        }

        String hash = passwordEncoder.encode(password);
        UsersRecord stored = userRepository.insert(username, email, hash);
        return toUser(stored);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username).map(UserService::toUser);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserService::toUser);
    }

    private static User toUser(UsersRecord record) {
        return new User(
                record.getId(),
                record.getUsername(),
                record.getEmail(),
                record.getBio(),
                Boolean.TRUE.equals(record.getPublicInventory()),
                record.getCreatedAt());
    }
}
