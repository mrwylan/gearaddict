package app.gearaddict.security;

import app.gearaddict.PostgresTestContainer;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class DatabaseUserDetailsServiceTest {

    @Autowired
    private DatabaseUserDetailsService detailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
        userService.register(new RegistrationRequest("alice", "alice@example.com", "password123"));
    }

    @Test
    void loadByEmailReturnsUserDetailsWithMatchingPassword() {
        UserDetails details = detailsService.loadUserByUsername("alice@example.com");

        assertThat(details.getUsername()).isEqualTo("alice@example.com");
        assertThat(passwordEncoder.matches("password123", details.getPassword())).isTrue();
        assertThat(details.getAuthorities()).extracting(Object::toString).contains("ROLE_USER");
    }

    @Test
    void loadByUsernameAlsoResolves() {
        UserDetails details = detailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice@example.com");
    }

    @Test
    void loadByUnknownThrows() {
        assertThatThrownBy(() -> detailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
