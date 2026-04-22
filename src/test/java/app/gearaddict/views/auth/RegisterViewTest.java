package app.gearaddict.views.auth;

import app.gearaddict.PostgresTestContainer;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class RegisterViewTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private final Routes routes = new Routes().autoDiscoverViews("app.gearaddict.views");

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
        MockVaadin.setup(routes);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
        SecurityContextHolder.clearContext();
    }

    private RegisterView openView() {
        RegisterView view = new RegisterView(userService);
        UI.getCurrent().add(view);
        return view;
    }

    @Test
    void successfulRegistrationPersistsUserAndAuthenticates() {
        openView();

        _get(TextField.class, spec -> spec.withId("username")).setValue("alice");
        _get(EmailField.class, spec -> spec.withId("email")).setValue("alice@example.com");
        _get(PasswordField.class, spec -> spec.withId("password")).setValue("password123");

        _click(_get(Button.class, spec -> spec.withId("submit")));

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("alice@example.com");
    }

    @Test
    void duplicateUsernameShowsFieldError() {
        userService.register(new RegistrationRequest("alice", "existing@example.com", "password123"));
        openView();

        _get(TextField.class, spec -> spec.withId("username")).setValue("alice");
        _get(EmailField.class, spec -> spec.withId("email")).setValue("new@example.com");
        _get(PasswordField.class, spec -> spec.withId("password")).setValue("password123");

        _click(_get(Button.class, spec -> spec.withId("submit")));

        TextField usernameField = _get(TextField.class, spec -> spec.withId("username"));
        assertThat(usernameField.isInvalid()).isTrue();
        assertThat(_get(Paragraph.class, spec -> spec.withId("error-message")).isVisible()).isTrue();
        assertThat(userRepository.findByEmail("new@example.com")).isEmpty();
    }

    @Test
    void duplicateEmailShowsFieldError() {
        userService.register(new RegistrationRequest("alice", "alice@example.com", "password123"));
        openView();

        _get(TextField.class, spec -> spec.withId("username")).setValue("bob");
        _get(EmailField.class, spec -> spec.withId("email")).setValue("alice@example.com");
        _get(PasswordField.class, spec -> spec.withId("password")).setValue("password123");

        _click(_get(Button.class, spec -> spec.withId("submit")));

        assertThat(_get(EmailField.class, spec -> spec.withId("email")).isInvalid()).isTrue();
    }

    @Test
    void shortPasswordShowsError() {
        openView();

        _get(TextField.class, spec -> spec.withId("username")).setValue("alice");
        _get(EmailField.class, spec -> spec.withId("email")).setValue("alice@example.com");
        _get(PasswordField.class, spec -> spec.withId("password")).setValue("short");

        _click(_get(Button.class, spec -> spec.withId("submit")));

        assertThat(_get(PasswordField.class, spec -> spec.withId("password")).isInvalid()).isTrue();
        assertThat(userRepository.findByEmail("alice@example.com")).isEmpty();
    }

    @Test
    void emptyFieldsShowValidationErrors() {
        openView();

        _click(_get(Button.class, spec -> spec.withId("submit")));

        assertThat(_get(TextField.class, spec -> spec.withId("username")).isInvalid()).isTrue();
        assertThat(_get(EmailField.class, spec -> spec.withId("email")).isInvalid()).isTrue();
        assertThat(_get(PasswordField.class, spec -> spec.withId("password")).isInvalid()).isTrue();
    }
}
