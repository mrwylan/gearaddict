package app.gearaddict.views.profile;

import app.gearaddict.KaribuSpringFixture;
import app.gearaddict.PostgresTestContainer;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.User;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import com.github.mvysny.kaributesting.v10.Routes;
import org.springframework.context.ApplicationContext;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class ProfileViewTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private final Routes routes = new Routes().autoDiscoverViews("app.gearaddict.views");

    private User alice;
    private AuthenticationContext auth;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        alice = userService.register(
                new RegistrationRequest("alice", "alice@example.com", "password123"));
        auth = mock(AuthenticationContext.class);
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(auth.getAuthenticatedUser(UserDetails.class)).thenReturn(Optional.of(principal));
        when(auth.getPrincipalName()).thenReturn(Optional.of("alice@example.com"));

        KaribuSpringFixture.setUp(applicationContext, routes);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringFixture.tearDown();
    }

    private ProfileView openView() {
        ProfileView view = new ProfileView(userService, auth);
        UI.getCurrent().add(view);
        return view;
    }

    @Test
    void formIsPrePopulatedWithCurrentUsernameAndBio() {
        userService.updateProfile(alice.id(), "alice", "Original bio.");

        openView();

        assertThat(_get(TextField.class, spec -> spec.withId("username")).getValue()).isEqualTo("alice");
        assertThat(_get(TextArea.class, spec -> spec.withId("bio")).getValue()).isEqualTo("Original bio.");
    }

    @Test
    void savePersistsChangesAndShowsNoError() {
        openView();

        _get(TextField.class, spec -> spec.withId("username")).setValue("alice-rack");
        _get(TextArea.class, spec -> spec.withId("bio")).setValue("Synth lover.");

        _click(_get(Button.class, spec -> spec.withId("save")));

        User reloaded = userService.findById(alice.id()).orElseThrow();
        assertThat(reloaded.username()).isEqualTo("alice-rack");
        assertThat(reloaded.bio()).isEqualTo("Synth lover.");
    }

    @Test
    void duplicateUsernameShowsFieldError() {
        userService.register(new RegistrationRequest("bob", "bob@example.com", "password123"));
        openView();

        _get(TextField.class, spec -> spec.withId("username")).setValue("bob");
        _click(_get(Button.class, spec -> spec.withId("save")));

        assertThat(_get(TextField.class, spec -> spec.withId("username")).isInvalid()).isTrue();
        assertThat(_get(Paragraph.class, spec -> spec.withId("error-message")).isVisible()).isTrue();
        assertThat(userService.findById(alice.id()).orElseThrow().username()).isEqualTo("alice");
    }

    @Test
    void emptyUsernameShowsFieldError() {
        openView();

        _get(TextField.class, spec -> spec.withId("username")).setValue("   ");
        _click(_get(Button.class, spec -> spec.withId("save")));

        assertThat(_get(TextField.class, spec -> spec.withId("username")).isInvalid()).isTrue();
    }

    @Test
    void tooLongBioShowsFieldError() {
        openView();

        _get(TextArea.class, spec -> spec.withId("bio")).setValue("x".repeat(501));
        _click(_get(Button.class, spec -> spec.withId("save")));

        assertThat(_get(TextArea.class, spec -> spec.withId("bio")).isInvalid()).isTrue();
    }

    @Test
    void visibilityToggleDefaultsToPrivateAndPersistsAfterSave() {
        openView();

        Checkbox toggle = _get(Checkbox.class, spec -> spec.withId("public-inventory-toggle"));
        assertThat(toggle.getValue()).isFalse();

        toggle.setValue(true);
        _click(_get(Button.class, spec -> spec.withId("save")));

        assertThat(userService.findById(alice.id()).orElseThrow().publicInventory()).isTrue();
    }

    @Test
    void cancelRevertsVisibilityToggleToOriginal() {
        userService.setInventoryVisibility(alice.id(), true);
        openView();

        Checkbox toggle = _get(Checkbox.class, spec -> spec.withId("public-inventory-toggle"));
        assertThat(toggle.getValue()).isTrue();
        toggle.setValue(false);

        _click(_get(Button.class, spec -> spec.withId("cancel")));

        assertThat(userService.findById(alice.id()).orElseThrow().publicInventory()).isTrue();
    }

    @Test
    void cancelDiscardsEditsInTheForm() {
        openView();

        TextField username = _get(TextField.class, spec -> spec.withId("username"));
        TextArea bio = _get(TextArea.class, spec -> spec.withId("bio"));
        username.setValue("not-saved");
        bio.setValue("not-saved-bio");

        _click(_get(Button.class, spec -> spec.withId("cancel")));

        assertThat(userService.findById(alice.id()).orElseThrow().username()).isEqualTo("alice");
    }
}
