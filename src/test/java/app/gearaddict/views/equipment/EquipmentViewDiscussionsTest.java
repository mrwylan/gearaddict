package app.gearaddict.views.equipment;

import app.gearaddict.KaribuSpringFixture;
import app.gearaddict.PostgresTestContainer;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.User;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import app.gearaddict.views.thread.ThreadView;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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

/**
 * UC-008 coverage for the discussions section embedded in the equipment page —
 * the entry point described in the use case's Main Success Scenario step 1.
 */
@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class EquipmentViewDiscussionsTest {

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

    // ------------------------------------------------------------------
    // UC-008 A1: No threads exist for this equipment
    // ------------------------------------------------------------------

    @Test
    void showsEmptyMessageWhenNoThreadsForEquipment() {
        UI.getCurrent().navigate(EquipmentView.class, minimoog.id());

        Paragraph empty = _get(Paragraph.class, spec -> spec.withId("equipment-discussions-empty"));
        assertThat(empty.getText()).isEqualTo("No discussions started yet.");
    }

    // ------------------------------------------------------------------
    // UC-008 Main Success Scenario Step 2: threads rendered with title,
    // author, date, reply count.
    // ------------------------------------------------------------------

    @Test
    void listsThreadsForEquipmentWithReplyCount() {
        long threadId = insertThread(minimoog.id(), alpha.id(),
                "Filter mod question",
                "Has anyone swapped the filter chip in their Minimoog?",
                LocalDateTime.of(2026, 4, 10, 10, 0),
                LocalDateTime.of(2026, 4, 20, 12, 0));
        insertReply(threadId, beta.id(), "Yes, the CA3080 swap works well.",
                LocalDateTime.of(2026, 4, 20, 12, 0));

        UI.getCurrent().navigate(EquipmentView.class, minimoog.id());

        Div list = _get(Div.class, spec -> spec.withId("equipment-discussion-list"));
        List<Anchor> anchors = _find(Anchor.class,
                spec -> spec.withText("Filter mod question"));
        assertThat(anchors).singleElement().satisfies(a ->
                assertThat(a.getHref()).isEqualTo(ThreadView.ROUTE + "/" + threadId));

        List<Span> replyCountLabels = list.getChildren()
                .flatMap(row -> row.getChildren())
                .filter(Span.class::isInstance)
                .map(Span.class::cast)
                .filter(s -> s.getText().contains("reply") || s.getText().contains("replies"))
                .toList();
        assertThat(replyCountLabels).extracting(Span::getText).contains("1 reply");
    }
}
