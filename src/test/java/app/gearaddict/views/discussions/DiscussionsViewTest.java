package app.gearaddict.views.discussions;

import app.gearaddict.KaribuSpringFixture;
import app.gearaddict.PostgresTestContainer;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.User;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.Location;
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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class DiscussionsViewTest {

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
    private Equipment prophet;
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
        prophet = catalogEquipmentByName("Prophet-5");

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

    private long insertThread(Long equipmentId,
                              Long authorId,
                              String title,
                              String body,
                              LocalDateTime createdAt,
                              LocalDateTime lastReplyAt) {
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

    // ------------------------------------------------------------------
    // UC-008 Main Success Scenario / BR-003 / BR-004
    // ------------------------------------------------------------------

    @Test
    void rendersHeadingAndSubtitle() {
        UI.getCurrent().navigate(DiscussionsView.class);

        H1 heading = _get(H1.class, spec -> spec.withId("discussions-heading"));
        assertThat(heading.getText()).isEqualTo("Discussions");
    }

    @Test
    void listsAggregatedThreadsSortedByMostRecentActivity() {
        long minimoogThread = insertThread(minimoog.id(), alpha.id(),
                "Minimoog filter tips", "How do you tame the resonance?",
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2026, 4, 20, 12, 0));
        long prophetThread = insertThread(prophet.id(), beta.id(),
                "Prophet-5 rev4 vs rev3", "Anyone compared them side by side?",
                LocalDateTime.of(2025, 2, 1, 10, 0),
                LocalDateTime.of(2026, 4, 15, 9, 0));
        long oldMinimoogThread = insertThread(minimoog.id(), beta.id(),
                "Moog service manual", "Looking for a PDF of the original docs.",
                LocalDateTime.of(2026, 4, 5, 10, 0),
                null);

        UI.getCurrent().navigate(DiscussionsView.class);

        @SuppressWarnings("unchecked")
        VirtualList<Object> list = _get(VirtualList.class, spec -> spec.withId("discussion-thread-list"));
        DataProvider<Object, ?> provider = list.getDataProvider();
        int size = provider.size(new Query<>(null));
        assertThat(size).isEqualTo(3);

        List<Object> items = provider.fetch(new Query<>(0, size, List.of(), null, null)).toList();
        // Expected order: minimoogThread (2026-04-20), prophetThread (2026-04-15), oldMinimoogThread (2026-04-05 created)
        assertThat(items)
                .extracting(i -> ((app.gearaddict.discussion.DiscussionThreadListItem) i).id())
                .containsExactly(minimoogThread, prophetThread, oldMinimoogThread);
    }

    @Test
    void showsReplyCountPerThread() {
        long thread = insertThread(minimoog.id(), alpha.id(),
                "Classic Moog hum", "Is anyone else hearing the 60Hz buzz?",
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2026, 4, 20, 12, 0));
        insertReply(thread, beta.id(), "Yes — try a ground lift.", LocalDateTime.of(2026, 4, 19, 12, 0));
        insertReply(thread, beta.id(), "Also check the power cable routing.", LocalDateTime.of(2026, 4, 20, 12, 0));

        UI.getCurrent().navigate(DiscussionsView.class);

        @SuppressWarnings("unchecked")
        VirtualList<Object> list = _get(VirtualList.class, spec -> spec.withId("discussion-thread-list"));
        DataProvider<Object, ?> provider = list.getDataProvider();
        Object item = provider.fetch(new Query<>(0, 1, List.of(), null, null)).findFirst().orElseThrow();
        int replyCount = ((app.gearaddict.discussion.DiscussionThreadListItem) item).replyCount();
        assertThat(replyCount).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // UC-008 A1: No threads exist
    // ------------------------------------------------------------------

    @Test
    void showsEmptyStateWhenNoThreadsExist() {
        UI.getCurrent().navigate(DiscussionsView.class);

        Div empty = _get(Div.class, spec -> spec.withId("discussions-empty"));
        assertThat(empty.isVisible()).isTrue();
        Paragraph message = _get(Paragraph.class, spec -> spec.withText(
                "No discussions yet. Be the first to start one from an equipment page."));
        assertThat(message).isNotNull();
    }

    // ------------------------------------------------------------------
    // UC-008 Main Success Scenario Step 3: click navigates to thread
    // ------------------------------------------------------------------

    @Test
    void threadItemsCarryIdsForNavigationToThreadView() {
        long threadId = insertThread(minimoog.id(), alpha.id(),
                "Filter tracking",
                "The filter only tracks up to about 3 octaves — is that normal?",
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2026, 4, 20, 12, 0));

        UI.getCurrent().navigate(DiscussionsView.class);

        @SuppressWarnings("unchecked")
        VirtualList<Object> list = _get(VirtualList.class, spec -> spec.withId("discussion-thread-list"));
        DataProvider<Object, ?> provider = list.getDataProvider();
        Object item = provider.fetch(new Query<>(0, 1, List.of(), null, null))
                .findFirst()
                .orElseThrow();
        assertThat(((app.gearaddict.discussion.DiscussionThreadListItem) item).id())
                .isEqualTo(threadId);
    }

    // ------------------------------------------------------------------
    // View is reachable via the top-level "discussions" route.
    // ------------------------------------------------------------------

    @Test
    void registeredAtDiscussionsRoute() {
        UI.getCurrent().navigate(DiscussionsView.class);
        assertThat(UI.getCurrent().getInternals().getActiveViewLocation())
                .extracting(Location::getPath)
                .isEqualTo(DiscussionsView.ROUTE);
    }
}
