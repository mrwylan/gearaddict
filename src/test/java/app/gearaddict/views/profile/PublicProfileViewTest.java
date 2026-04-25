package app.gearaddict.views.profile;

import app.gearaddict.KaribuSpringFixture;
import app.gearaddict.PostgresTestContainer;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentCategory;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.gear.GearItemFormData;
import app.gearaddict.gear.GearItemService;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.User;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import app.gearaddict.views.equipment.EquipmentView;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class PublicProfileViewTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GearItemService gearItemService;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private ApplicationContext applicationContext;

    private final Routes routes = new Routes().autoDiscoverViews("app.gearaddict.views");

    private User publicAlice;
    private User privateBob;
    private Equipment minimoog;
    private Equipment prophet;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        publicAlice = userService.register(
                new RegistrationRequest("alice", "alice@example.com", "password123"));
        userService.updateProfile(publicAlice.id(), "alice", "Synth aficionado.");
        publicAlice = userService.setInventoryVisibility(publicAlice.id(), true);

        privateBob = userService.register(
                new RegistrationRequest("bob", "bob@example.com", "password123"));

        minimoog = catalogEquipmentByName("Minimoog Model D");
        prophet = catalogEquipmentByName("Prophet-5");

        KaribuSpringFixture.setUp(applicationContext, routes);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringFixture.tearDown();
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
     * Bypasses Spring Security routing by instantiating the view directly and invoking
     * {@code setParameter} — same pattern as {@code InventoryViewTest.openView()}. The
     * view's {@code setParameter} only reads the {@code userId} argument, never the
     * {@link BeforeEvent}, so passing {@code null} is safe.
     */
    private PublicProfileView openView(Long userId) {
        PublicProfileView view = new PublicProfileView(userService, gearItemService);
        view.setParameter((BeforeEvent) null, userId);
        UI.getCurrent().add(view);
        return view;
    }

    // ------------------------------------------------------------------
    // UC-013 A2: Profile not found
    // ------------------------------------------------------------------

    @Test
    void unknownUserShowsNotFoundHeading() {
        openView(9_999_999L);

        H1 notFound = _get(H1.class, spec -> spec.withId("user-not-found"));
        assertThat(notFound.getText()).isEqualTo("User not found");
    }

    // ------------------------------------------------------------------
    // UC-013 A1: Target user has no public inventory
    // ------------------------------------------------------------------

    @Test
    void privateInventoryShowsPrivateNoteAndHidesGear() {
        // Bob has gear but his inventory is private (default).
        gearItemService.add(privateBob.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));

        openView(privateBob.id());

        Paragraph note = _get(Paragraph.class, spec -> spec.withId("private-inventory-note"));
        assertThat(note.getText()).contains("private");
        assertThat(_find(Div.class, spec -> spec.withId("public-gear-grid"))).isEmpty();
    }

    @Test
    void publicButEmptyInventoryShowsEmptyMessage() {
        openView(publicAlice.id());

        Paragraph empty = _get(Paragraph.class, spec -> spec.withId("public-inventory-empty"));
        assertThat(empty.getText()).contains("alice").contains("hasn't added any gear");
    }

    // ------------------------------------------------------------------
    // UC-013 Main Success Scenario steps 2-3
    // ------------------------------------------------------------------

    @Test
    void publicProfileShowsUsernameBioAndInventoryCards() {
        gearItemService.add(publicAlice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));
        gearItemService.add(publicAlice.id(),
                new GearItemFormData(prophet.id(), null, EquipmentCategory.SYNTH, null));

        openView(publicAlice.id());

        assertThat(_get(H1.class, spec -> spec.withId("public-username")).getText()).isEqualTo("alice");
        assertThat(_get(Paragraph.class, spec -> spec.withId("public-bio")).getText())
                .isEqualTo("Synth aficionado.");

        Div grid = _get(Div.class, spec -> spec.withId("public-gear-grid"));
        assertThat(grid.getChildren().count()).isEqualTo(2);
        String html = grid.getElement().getOuterHTML();
        assertThat(html)
                .contains("Minimoog Model D")
                .contains("Moog")
                .contains("Synth")
                .contains("Prophet-5")
                .contains("Sequential");
    }

    // ------------------------------------------------------------------
    // UC-013 BR-002: No edit access
    // ------------------------------------------------------------------

    @Test
    void publicProfileExposesNoEditOrRemoveButtons() {
        gearItemService.add(publicAlice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));

        openView(publicAlice.id());

        assertThat(_find(com.vaadin.flow.component.button.Button.class,
                spec -> spec.withCaption("Edit"))).isEmpty();
        assertThat(_find(com.vaadin.flow.component.button.Button.class,
                spec -> spec.withCaption("Remove"))).isEmpty();
        assertThat(_find(com.vaadin.flow.component.button.Button.class,
                spec -> spec.withId("add-gear"))).isEmpty();
    }

    // ------------------------------------------------------------------
    // UC-013 Main Success Scenario step 5: equipment deep-link (UC-007)
    // ------------------------------------------------------------------

    @Test
    void catalogLinkedItemRendersAnchorToEquipmentPage() {
        gearItemService.add(publicAlice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));

        openView(publicAlice.id());

        List<Anchor> anchors = _find(Anchor.class, spec -> spec.withText("Minimoog Model D"));
        assertThat(anchors).singleElement().satisfies(a ->
                assertThat(a.getHref())
                        .isEqualTo(EquipmentView.ROUTE + "/" + minimoog.id()));
    }

    @Test
    void freeTextItemDoesNotRenderEquipmentLink() {
        gearItemService.add(publicAlice.id(),
                new GearItemFormData(null, "Handbuilt Fuzz", EquipmentCategory.EFFECT, null));

        openView(publicAlice.id());

        // Free-text card renders the name as a heading, never as an anchor.
        assertThat(_find(Anchor.class, spec -> spec.withText("Handbuilt Fuzz"))).isEmpty();

        Div grid = _get(Div.class, spec -> spec.withId("public-gear-grid"));
        assertThat(grid.getElement().getTextRecursively()).contains("Handbuilt Fuzz");
        assertThat(_find(H3.class)).extracting(H3::getText).contains("Handbuilt Fuzz");
    }

    // ------------------------------------------------------------------
    // UC-013 BR-003: Infinite scroll lazy loading
    // ------------------------------------------------------------------

    @Test
    void initialBatchIsCappedAndLoadMoreAppendsRemaining() {
        int total = PublicProfileView.PAGE_SIZE + 3;
        for (int i = 0; i < total; i++) {
            String name = String.format("Custom Rig %03d", i);
            gearItemService.add(publicAlice.id(),
                    new GearItemFormData(null, name, EquipmentCategory.OTHER, null));
        }

        PublicProfileView view = openView(publicAlice.id());

        Div grid = _get(Div.class, spec -> spec.withId("public-gear-grid"));
        assertThat(grid.getChildren().count()).isEqualTo(PublicProfileView.PAGE_SIZE);

        view.loadMore();
        assertThat(grid.getChildren().count()).isEqualTo(total);

        // No-op once exhausted.
        view.loadMore();
        assertThat(grid.getChildren().count()).isEqualTo(total);
    }
}
