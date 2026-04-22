package app.gearaddict.user;

import app.gearaddict.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void registerStoresUserWithBcryptPasswordAtCostFactorAtLeast12() {
        User user = userService.register(
                new RegistrationRequest("alice", "alice@example.com", "secret-pw-123"));

        assertThat(user.id()).isNotNull();
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.email()).isEqualTo("alice@example.com");
        assertThat(user.publicInventory()).isFalse();
        assertThat(user.createdAt()).isNotNull();

        String storedHash = userRepository.findByEmail("alice@example.com").orElseThrow().getPassword();
        assertThat(storedHash).startsWith("$2");
        int cost = Integer.parseInt(storedHash.substring(4, 6));
        assertThat(cost).isGreaterThanOrEqualTo(UserService.BCRYPT_COST);
        assertThat(BCrypt.checkpw("secret-pw-123", storedHash)).isTrue();
        assertThat(passwordEncoder.matches("secret-pw-123", storedHash)).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsernameCaseInsensitive() {
        userService.register(new RegistrationRequest("alice", "alice@example.com", "password123"));

        assertThatThrownBy(() -> userService.register(
                new RegistrationRequest("ALICE", "other@example.com", "password123")))
                .isInstanceOf(RegistrationException.class)
                .extracting("reason")
                .isEqualTo(RegistrationException.Reason.USERNAME_TAKEN);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        userService.register(new RegistrationRequest("alice", "alice@example.com", "password123"));

        assertThatThrownBy(() -> userService.register(
                new RegistrationRequest("bob", "alice@example.com", "password123")))
                .isInstanceOf(RegistrationException.class)
                .extracting("reason")
                .isEqualTo(RegistrationException.Reason.EMAIL_TAKEN);
    }

    @Test
    void registerRejectsShortPassword() {
        assertThatThrownBy(() -> userService.register(
                new RegistrationRequest("bob", "bob@example.com", "short")))
                .isInstanceOf(RegistrationException.class)
                .extracting("reason")
                .isEqualTo(RegistrationException.Reason.PASSWORD_TOO_SHORT);
    }

    @Test
    void registerRejectsEmptyFields() {
        assertThatThrownBy(() -> userService.register(
                new RegistrationRequest("", "bob@example.com", "password123")))
                .isInstanceOf(RegistrationException.class)
                .extracting("reason")
                .isEqualTo(RegistrationException.Reason.MISSING_FIELD);

        assertThatThrownBy(() -> userService.register(
                new RegistrationRequest("bob", "", "password123")))
                .isInstanceOf(RegistrationException.class)
                .extracting("reason")
                .isEqualTo(RegistrationException.Reason.MISSING_FIELD);

        assertThatThrownBy(() -> userService.register(
                new RegistrationRequest("bob", "bob@example.com", "")))
                .isInstanceOf(RegistrationException.class)
                .extracting("reason")
                .isEqualTo(RegistrationException.Reason.MISSING_FIELD);
    }

    @Test
    void registerRejectsInvalidEmail() {
        assertThatThrownBy(() -> userService.register(
                new RegistrationRequest("bob", "not-an-email", "password123")))
                .isInstanceOf(RegistrationException.class)
                .extracting("reason")
                .isEqualTo(RegistrationException.Reason.INVALID_EMAIL);
    }

    @Test
    void findByEmailReturnsUserWhenPresent() {
        userService.register(new RegistrationRequest("alice", "alice@example.com", "password123"));

        assertThat(userService.findByEmail("alice@example.com"))
                .map(User::username)
                .contains("alice");
        assertThat(userService.findByEmail("missing@example.com")).isEmpty();
    }
}
