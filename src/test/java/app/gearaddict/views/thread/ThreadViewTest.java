package app.gearaddict.views.thread;

import app.gearaddict.KaribuSpringFixture;
import app.gearaddict.PostgresTestContainer;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.User;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import app.gearaddict.views.discussions.DiscussionsView;
import app.gearaddict.views.equipment.EquipmentView;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class ThreadViewTest {

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
    private User alpha;
    private User beta;

    @BeforeEach
    void setUp() {
        clearDiscussionTables();
        userRepository.deleteAll();
        alpha = userService.register(
                new RegistrationRequest("alpha_author", "alpha@example.com", "password123"));
        beta = userService.register(
                new RegistrationRequest("beta_author", "beta@example.com", "password123"));

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

    private long insertThread(Long equipmentId, Long authorId, String title, String body,
                              LocalDateTime createdAt, LocalDateTime lastReplyAt) {
        return jdbc.queryForObject(
                "INSERT INTO discussion_thread (equipment_id, author_id, title, body, created_at, last_reply_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                equipmentId, authorId, title, body, createdAt, lastReplyAt);
    }

    private void insertReply(Long threadId, Long authorId, String body, LocalDateTime createdAt) {
        jdbc.update(
                "INSERT INTO reply (discussion_thread_id, author_id, body, created_at) VALUES (?, ?, ?, ?)",
                threadId, authorId, body, createdAt);
    }

    private void navigateToThread(long threadId) {
        UI.getCurrent().navigate(ThreadView.class,
                new RouteParameters("threadID", String.valueOf(threadId)));
    }

    // ------------------------------------------------------------------
    // UC-008 Main Success Scenario Step 4: opening post rendered
    // ------------------------------------------------------------------

    @Test
    void rendersOpeningPostWithTitleAuthorAndBody() {
        long threadId = insertThread(minimoog.id(), alpha.id(),
                "Classic Moog hum",
                "Is anyone else hearing 60Hz buzz at high resonance?",
                LocalDateTime.of(2026, 4, 1, 10, 0),
                null);

        UI.getCurrent().navigate(ThreadView.class, threadId);

        H1 title = _get(H1.class, spec -> spec.withId("thread-title"));
        assertThat(title.getText()).isEqualTo("Classic Moog hum");

        Span author = _get(Span.class, spec -> spec.withId("thread-author"));
        assertThat(author.getText()).contains("alpha_author");

        Paragraph body = _get(Paragraph.class, spec -> spec.withId("thread-body"));
        assertThat(body.getText()).isEqualTo("Is anyone else hearing 60Hz buzz at high resonance?");
    }

    // ------------------------------------------------------------------
    // UC-008 BR-002: replies displayed oldest first
    // ------------------------------------------------------------------

    @Test
    void rendersRepliesInChronologicalOrder() {
        long threadId = insertThread(minimoog.id(), alpha.id(),
                "Classic Moog hum",
                "Is anyone else hearing 60Hz buzz at high resonance?",
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 3, 10, 0));
        insertReply(threadId, beta.id(), "Try a ground lift adapter.",
                LocalDateTime.of(2026, 4, 2, 10, 0));
        insertReply(threadId, alpha.id(), "Ground lift worked, thanks!",
                LocalDateTime.of(2026, 4, 2, 14, 0));
        insertReply(threadId, beta.id(), "Also move your wall wart further away.",
                LocalDateTime.of(2026, 4, 3, 10, 0));

        UI.getCurrent().navigate(ThreadView.class, threadId);

        @SuppressWarnings("unchecked")
        VirtualList<Object> list = _get(VirtualList.class, spec -> spec.withId("reply-list"));
        DataProvider<Object, ?> provider = list.getDataProvider();
        List<Object> items = provider.fetch(new Query<>(0, 10, List.of(), null, null)).toList();
        assertThat(items).hasSize(3);
        assertThat(items)
                .extracting(i -> ((app.gearaddict.discussion.DiscussionReply) i).body())
                .containsExactly(
                        "Try a ground lift adapter.",
                        "Ground lift worked, thanks!",
                        "Also move your wall wart further away.");
    }

    // ------------------------------------------------------------------
    // Reply count header reflects total replies
    // ------------------------------------------------------------------

    @Test
    void repliesHeadingShowsCount() {
        long threadId = insertThread(minimoog.id(), alpha.id(),
                "Classic Moog hum", "Is anyone else hearing 60Hz buzz at high resonance?",
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 3, 10, 0));
        insertReply(threadId, beta.id(), "Try a ground lift.", LocalDateTime.of(2026, 4, 2, 10, 0));
        insertReply(threadId, beta.id(), "Also check wall warts.", LocalDateTime.of(2026, 4, 3, 10, 0));

        UI.getCurrent().navigate(ThreadView.class, threadId);

        H3 heading = _find(H3.class).stream()
                .filter(h -> h.getText().startsWith("Replies"))
                .findFirst()
                .orElseThrow();
        assertThat(heading.getText()).isEqualTo("Replies (2)");
    }

    // ------------------------------------------------------------------
    // Thread with no replies shows the empty replies message
    // ------------------------------------------------------------------

    @Test
    void showsEmptyRepliesStateWhenNoReplies() {
        long threadId = insertThread(minimoog.id(), alpha.id(),
                "Brand new thread",
                "Just got my Minimoog, any first-day tips?",
                LocalDateTime.of(2026, 4, 20, 10, 0),
                null);

        UI.getCurrent().navigate(ThreadView.class, threadId);

        Paragraph empty = _get(Paragraph.class, spec -> spec.withId("replies-empty"));
        assertThat(empty.getText()).isEqualTo("No replies yet.");
        assertThat(_find(VirtualList.class, spec -> spec.withId("reply-list"))).isEmpty();
    }

    // ------------------------------------------------------------------
    // UC-008 A2: Thread Not Found
    // ------------------------------------------------------------------

    @Test
    void showsNotFoundWhenThreadMissing() {
        UI.getCurrent().navigate(ThreadView.class, 999_999L);

        H1 notFound = _get(H1.class, spec -> spec.withId("thread-not-found"));
        assertThat(notFound.getText()).isEqualTo("Thread not found");

        RouterLink back = _get(RouterLink.class, spec -> spec.withId("back-to-discussions"));
        assertThat(back.getHref()).isEqualTo(DiscussionsView.ROUTE);
    }

    // ------------------------------------------------------------------
    // Breadcrumbs link back to Discussions and the Equipment page
    // ------------------------------------------------------------------

    @Test
    void breadcrumbsLinkToDiscussionsAndEquipmentPage() {
        long threadId = insertThread(minimoog.id(), alpha.id(),
                "Classic Moog hum", "Is anyone else hearing 60Hz buzz?",
                LocalDateTime.of(2026, 4, 1, 10, 0), null);

        UI.getCurrent().navigate(ThreadView.class, threadId);

        RouterLink toDiscussions = _get(RouterLink.class,
                spec -> spec.withId("breadcrumb-discussions"));
        assertThat(toDiscussions.getHref()).isEqualTo(DiscussionsView.ROUTE);

        RouterLink toEquipment = _get(RouterLink.class,
                spec -> spec.withId("breadcrumb-equipment"));
        assertThat(toEquipment.getHref()).isEqualTo(EquipmentView.ROUTE + "/" + minimoog.id());
        assertThat(toEquipment.getElement().getText())
                .contains(minimoog.manufacturer())
                .contains(minimoog.name());
    }
}
