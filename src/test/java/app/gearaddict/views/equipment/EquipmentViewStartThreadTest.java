package app.gearaddict.views.equipment;

import app.gearaddict.KaribuSpringFixture;
import app.gearaddict.PostgresTestContainer;
import app.gearaddict.discussion.DiscussionThreadService;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.User;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UC-015 coverage on the equipment page entry point: gating, the new-thread dialog,
 * validation, and the post-success navigation to ThreadView.
 */
@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class EquipmentViewStartThreadTest {

    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private DiscussionThreadService discussionThreadService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JdbcTemplate jdbc;

    private final Routes routes = new Routes().autoDiscoverViews("app.gearaddict.views");

    private Equipment minimoog;
    private User alice;
    private AuthenticationContext authForAlice;
    private AuthenticationContext anonAuth;

    @BeforeEach
    void setUp() {
        clearDiscussionTables();
        userRepository.deleteAll();
        alice = userService.register(
                new RegistrationRequest("alice", "alice@example.com", "password123"));

        UserDetails alicePrincipal = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        authForAlice = mock(AuthenticationContext.class);
        when(authForAlice.getAuthenticatedUser(UserDetails.class))
                .thenReturn(Optional.of(alicePrincipal));

        anonAuth = mock(AuthenticationContext.class);
        when(anonAuth.getAuthenticatedUser(UserDetails.class)).thenReturn(Optional.empty());

        minimoog = catalogEquipmentByName("Minimoog Model D");

        KaribuSpringFixture.setUp(applicationContext, routes);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringFixture.tearDown();
        clearDiscussionTables();
    }

    private void clearDiscussionTables() {
        jdbc.update("DELETE FROM reply");
        jdbc.update("DELETE FROM discussion_thread");
    }

    private Equipment catalogEquipmentByName(String name) {
        return equipmentService
                .browse(Optional.empty(), Optional.of(name), 0, 10)
                .stream()
                .filter(e -> e.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seeded equipment missing: " + name));
    }

    /**
     * Bypasses Spring Security routing — instantiates the view with a controllable
     * {@link AuthenticationContext}. Same pattern as {@code PublicProfileViewTest}.
     */
    private EquipmentView openView(AuthenticationContext auth, Long equipmentId) {
        EquipmentView view = new EquipmentView(
                equipmentService, discussionThreadService, userService, auth);
        view.setParameter((BeforeEvent) null, equipmentId);
        UI.getCurrent().add(view);
        return view;
    }

    // ------------------------------------------------------------------
    // UC-015 BR-001 / A3: only authenticated users see the CTA
    // ------------------------------------------------------------------

    @Test
    void anonymousVisitorDoesNotSeeStartDiscussionButton() {
        openView(anonAuth, minimoog.id());

        assertThat(_find(Button.class, spec -> spec.withId("start-discussion"))).isEmpty();
    }

    @Test
    void authenticatedUserSeesStartDiscussionButton() {
        openView(authForAlice, minimoog.id());

        assertThat(_get(Button.class, spec -> spec.withId("start-discussion")).isVisible()).isTrue();
    }

    // ------------------------------------------------------------------
    // UC-015 Main Success Scenario step 2: dialog with title + body fields
    // ------------------------------------------------------------------

    @Test
    void clickingStartDiscussionOpensDialogWithEmptyFields() {
        openView(authForAlice, minimoog.id());

        _click(_get(Button.class, spec -> spec.withId("start-discussion")));

        Dialog dialog = _get(Dialog.class, spec -> spec.withId("new-thread-dialog"));
        assertThat(dialog.isOpened()).isTrue();
        assertThat(_get(TextField.class, spec -> spec.withId("new-thread-title")).getValue()).isEmpty();
        assertThat(_get(TextArea.class, spec -> spec.withId("new-thread-body")).getValue()).isEmpty();
    }

    // ------------------------------------------------------------------
    // UC-015 Main Success Scenario steps 4-7: persist + navigate
    // ------------------------------------------------------------------

    @Test
    void savingValidThreadPersistsItAndShowsCardOnEquipmentPage() {
        openView(authForAlice, minimoog.id());
        _click(_get(Button.class, spec -> spec.withId("start-discussion")));

        _get(TextField.class, spec -> spec.withId("new-thread-title"))
                .setValue("Filter mod question");
        _get(TextArea.class, spec -> spec.withId("new-thread-body"))
                .setValue("Has anyone swapped the filter chip in their Minimoog?");

        _click(_get(Button.class, spec -> spec.withId("new-thread-save")));

        assertThat(discussionThreadService.countForEquipment(minimoog.id())).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // UC-015 A1: Missing title or body
    // ------------------------------------------------------------------

    @Test
    void missingTitleShowsFieldErrorAndDoesNotPersist() {
        openView(authForAlice, minimoog.id());
        _click(_get(Button.class, spec -> spec.withId("start-discussion")));

        _get(TextArea.class, spec -> spec.withId("new-thread-body"))
                .setValue("This is a perfectly long opening post that meets the minimum length.");
        _click(_get(Button.class, spec -> spec.withId("new-thread-save")));

        TextField title = _get(TextField.class, spec -> spec.withId("new-thread-title"));
        assertThat(title.isInvalid()).isTrue();
        assertThat(discussionThreadService.countForEquipment(minimoog.id())).isZero();
    }

    @Test
    void missingBodyShowsFieldErrorAndDoesNotPersist() {
        openView(authForAlice, minimoog.id());
        _click(_get(Button.class, spec -> spec.withId("start-discussion")));

        _get(TextField.class, spec -> spec.withId("new-thread-title"))
                .setValue("A perfectly fine title");
        _click(_get(Button.class, spec -> spec.withId("new-thread-save")));

        TextArea body = _get(TextArea.class, spec -> spec.withId("new-thread-body"));
        assertThat(body.isInvalid()).isTrue();
        assertThat(discussionThreadService.countForEquipment(minimoog.id())).isZero();
    }

    // ------------------------------------------------------------------
    // UC-015 A2 / BR-002, BR-003: length limits
    // ------------------------------------------------------------------

    @Test
    void titleTooShortShowsFieldErrorAndDoesNotPersist() {
        openView(authForAlice, minimoog.id());
        _click(_get(Button.class, spec -> spec.withId("start-discussion")));

        _get(TextField.class, spec -> spec.withId("new-thread-title"))
                .setValue("Hey"); // < 5 chars
        _get(TextArea.class, spec -> spec.withId("new-thread-body"))
                .setValue("This opening post is well past the 10-character minimum.");
        _click(_get(Button.class, spec -> spec.withId("new-thread-save")));

        assertThat(_get(TextField.class, spec -> spec.withId("new-thread-title")).isInvalid()).isTrue();
        assertThat(discussionThreadService.countForEquipment(minimoog.id())).isZero();
    }

    @Test
    void bodyTooShortShowsFieldErrorAndDoesNotPersist() {
        openView(authForAlice, minimoog.id());
        _click(_get(Button.class, spec -> spec.withId("start-discussion")));

        _get(TextField.class, spec -> spec.withId("new-thread-title"))
                .setValue("A solid title here");
        _get(TextArea.class, spec -> spec.withId("new-thread-body"))
                .setValue("too short"); // < 10 chars
        _click(_get(Button.class, spec -> spec.withId("new-thread-save")));

        assertThat(_get(TextArea.class, spec -> spec.withId("new-thread-body")).isInvalid()).isTrue();
        assertThat(discussionThreadService.countForEquipment(minimoog.id())).isZero();
    }

    // ------------------------------------------------------------------
    // UC-015 dialog cancel
    // ------------------------------------------------------------------

    @Test
    void cancelClosesDialogWithoutPersisting() {
        openView(authForAlice, minimoog.id());
        _click(_get(Button.class, spec -> spec.withId("start-discussion")));

        Dialog dialog = _get(Dialog.class, spec -> spec.withId("new-thread-dialog"));
        _get(TextField.class, spec -> spec.withId("new-thread-title"))
                .setValue("Draft I'm not yet sure about");
        _get(TextArea.class, spec -> spec.withId("new-thread-body"))
                .setValue("This is a draft I'm reconsidering.");

        _click(_get(Button.class, spec -> spec.withId("new-thread-cancel")));

        assertThat(dialog.isOpened()).isFalse();
        assertThat(discussionThreadService.countForEquipment(minimoog.id())).isZero();
    }
}
