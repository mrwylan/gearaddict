package app.gearaddict.views.thread;

import app.gearaddict.KaribuSpringFixture;
import app.gearaddict.PostgresTestContainer;
import app.gearaddict.discussion.DiscussionReply;
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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
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
 * UC-016 coverage on the thread detail view: gating, validation, persistence,
 * and chronological ordering of replies.
 */
@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class ThreadViewReplyTest {

    @Autowired
    private DiscussionThreadService discussionThreadService;
    @Autowired
    private EquipmentService equipmentService;
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
    private User bob;
    private Long threadId;
    private AuthenticationContext authForAlice;
    private AuthenticationContext anonAuth;

    @BeforeEach
    void setUp() {
        clearDiscussionTables();
        userRepository.deleteAll();
        alice = userService.register(
                new RegistrationRequest("alice", "alice@example.com", "password123"));
        bob = userService.register(
                new RegistrationRequest("bob", "bob@example.com", "password123"));

        UserDetails alicePrincipal = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        authForAlice = mock(AuthenticationContext.class);
        when(authForAlice.getAuthenticatedUser(UserDetails.class))
                .thenReturn(Optional.of(alicePrincipal));

        anonAuth = mock(AuthenticationContext.class);
        when(anonAuth.getAuthenticatedUser(UserDetails.class)).thenReturn(Optional.empty());

        minimoog = catalogEquipmentByName("Minimoog Model D");
        threadId = discussionThreadService.startThread(
                minimoog.id(), bob.id(),
                "Filter mod question",
                "Has anyone swapped the filter chip in their Minimoog?");

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

    private ThreadView openView(AuthenticationContext auth, Long id) {
        ThreadView view = new ThreadView(discussionThreadService, userService, auth);
        view.setParameter((BeforeEvent) null, id);
        UI.getCurrent().add(view);
        return view;
    }

    // ------------------------------------------------------------------
    // UC-016 BR-001 / A3: only authenticated users get the reply form
    // ------------------------------------------------------------------

    @Test
    void anonymousVisitorDoesNotSeeReplyForm() {
        openView(anonAuth, threadId);

        assertThat(_find(VerticalLayout.class, spec -> spec.withId("reply-form"))).isEmpty();
        assertThat(_find(Button.class, spec -> spec.withId("reply-submit"))).isEmpty();
    }

    @Test
    void authenticatedUserSeesReplyForm() {
        openView(authForAlice, threadId);

        assertThat(_get(VerticalLayout.class, spec -> spec.withId("reply-form")).isVisible()).isTrue();
        assertThat(_get(TextArea.class, spec -> spec.withId("reply-body")).isVisible()).isTrue();
        assertThat(_get(Button.class, spec -> spec.withId("reply-submit")).isVisible()).isTrue();
    }

    // ------------------------------------------------------------------
    // UC-016 Main Success Scenario steps 4-7: persist, refresh, clear
    // ------------------------------------------------------------------

    @Test
    void postingValidReplyPersistsClearsFieldAndUpdatesHeading() {
        openView(authForAlice, threadId);

        // Empty-state paragraph is visible up-front because no replies exist yet.
        Paragraph emptyState = _get(Paragraph.class, spec -> spec.withId("replies-empty"));
        assertThat(emptyState.isVisible()).isTrue();

        _get(TextArea.class, spec -> spec.withId("reply-body"))
                .setValue("The CA3080 swap works well in mine.");
        _click(_get(Button.class, spec -> spec.withId("reply-submit")));

        assertThat(discussionThreadService.countReplies(threadId)).isEqualTo(1);
        List<DiscussionReply> replies = discussionThreadService.listReplies(threadId, 0, 10);
        assertThat(replies).singleElement().satisfies(r -> {
            assertThat(r.body()).isEqualTo("The CA3080 swap works well in mine.");
            assertThat(r.authorUsername()).isEqualTo("alice");
        });

        // Field clears so the user can start a follow-up reply.
        assertThat(_get(TextArea.class, spec -> spec.withId("reply-body")).getValue()).isEmpty();

        // Heading reflects the new count.
        assertThat(_get(H3.class, spec -> spec.withId("replies-heading")).getText())
                .isEqualTo("Replies (1)");

        // Empty-state paragraph hides once a reply exists.
        assertThat(emptyState.isVisible()).isFalse();
    }

    // ------------------------------------------------------------------
    // UC-016 A1: Empty reply
    // ------------------------------------------------------------------

    @Test
    void emptyReplyShowsFieldErrorAndDoesNotPersist() {
        openView(authForAlice, threadId);

        _click(_get(Button.class, spec -> spec.withId("reply-submit")));

        assertThat(_get(TextArea.class, spec -> spec.withId("reply-body")).isInvalid()).isTrue();
        assertThat(discussionThreadService.countReplies(threadId)).isZero();
    }

    @Test
    void whitespaceOnlyReplyShowsFieldErrorAndDoesNotPersist() {
        openView(authForAlice, threadId);

        _get(TextArea.class, spec -> spec.withId("reply-body")).setValue("   \n\t   ");
        _click(_get(Button.class, spec -> spec.withId("reply-submit")));

        assertThat(_get(TextArea.class, spec -> spec.withId("reply-body")).isInvalid()).isTrue();
        assertThat(discussionThreadService.countReplies(threadId)).isZero();
    }

    // ------------------------------------------------------------------
    // UC-016 BR-003: chronological ordering of replies
    // ------------------------------------------------------------------

    @Test
    void multipleRepliesAppearInChronologicalOrder() {
        openView(authForAlice, threadId);

        TextArea body = _get(TextArea.class, spec -> spec.withId("reply-body"));
        Button submit = _get(Button.class, spec -> spec.withId("reply-submit"));

        body.setValue("First reply.");
        _click(submit);
        body.setValue("Second reply.");
        _click(submit);
        body.setValue("Third reply.");
        _click(submit);

        List<DiscussionReply> replies = discussionThreadService.listReplies(threadId, 0, 10);
        assertThat(replies).extracting(DiscussionReply::body)
                .containsExactly("First reply.", "Second reply.", "Third reply.");

        assertThat(_get(H3.class, spec -> spec.withId("replies-heading")).getText())
                .isEqualTo("Replies (3)");
    }

    // ------------------------------------------------------------------
    // UC-016 success postcondition: thread last_reply_at is updated
    // ------------------------------------------------------------------

    @Test
    void postingReplyBumpsLastReplyAtTimestamp() {
        openView(authForAlice, threadId);

        Object before = jdbc.queryForObject(
                "SELECT last_reply_at FROM discussion_thread WHERE id = ?",
                Object.class, threadId);
        assertThat(before).isNull();

        _get(TextArea.class, spec -> spec.withId("reply-body")).setValue("First public response.");
        _click(_get(Button.class, spec -> spec.withId("reply-submit")));

        Object after = jdbc.queryForObject(
                "SELECT last_reply_at FROM discussion_thread WHERE id = ?",
                Object.class, threadId);
        assertThat(after).isNotNull();
    }
}
