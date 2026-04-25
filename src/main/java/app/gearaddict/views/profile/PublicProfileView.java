package app.gearaddict.views.profile;

import app.gearaddict.gear.GearItem;
import app.gearaddict.gear.GearItemService;
import app.gearaddict.user.User;
import app.gearaddict.user.UserService;
import app.gearaddict.views.MainLayout;
import app.gearaddict.views.equipment.EquipmentView;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Optional;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Profile — GearAddict")
@PermitAll
public class PublicProfileView extends VerticalLayout implements HasUrlParameter<Long> {

    public static final String ROUTE = "users";
    static final int PAGE_SIZE = 24;

    private final transient UserService userService;
    private final transient GearItemService gearItemService;

    private final Div gearGrid = new Div();
    private final Div loadSentinel = new Div();

    private Long targetUserId;
    private int loadedCount;
    private boolean allLoaded;

    public PublicProfileView(UserService userService, GearItemService gearItemService) {
        this.userService = userService;
        this.gearItemService = gearItemService;

        addClassName("public-profile-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long userId) {
        removeAll();
        loadedCount = 0;
        allLoaded = false;
        targetUserId = null;

        if (userId == null) {
            renderNotFound();
            return;
        }

        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            renderNotFound();
            return;
        }

        renderProfile(user.get());
    }

    private void renderNotFound() {
        H1 title = new H1("User not found");
        title.setId("user-not-found");
        Paragraph message = new Paragraph(
                "The profile you are looking for does not exist or has been removed.");
        add(title, message);
    }

    private void renderProfile(User user) {
        H1 title = new H1(user.username());
        title.setId("public-username");

        if (user.bio() != null && !user.bio().isBlank()) {
            Paragraph bio = new Paragraph(user.bio());
            bio.setId("public-bio");
            bio.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(title, bio);
        } else {
            add(title);
        }

        if (!user.publicInventory()) {
            Paragraph privateNote = new Paragraph(
                    "This user has chosen to keep their inventory private.");
            privateNote.setId("private-inventory-note");
            privateNote.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(privateNote);
            return;
        }

        targetUserId = user.id();
        configureGrid();
        add(gearGrid, loadSentinel);

        List<GearItem> firstBatch = fetchNextBatch();
        if (firstBatch.isEmpty()) {
            Paragraph empty = new Paragraph(user.username() + " hasn't added any gear yet.");
            empty.setId("public-inventory-empty");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(empty);
            gearGrid.setVisible(false);
            return;
        }

        firstBatch.forEach(item -> gearGrid.add(buildCard(item)));
        if (!allLoaded) {
            installInfiniteScroll();
        }
    }

    private void configureGrid() {
        gearGrid.setId("public-gear-grid");
        gearGrid.getStyle().set("display", "grid");
        gearGrid.getStyle().set("grid-template-columns", "repeat(auto-fill, minmax(260px, 1fr))");
        gearGrid.getStyle().set("gap", "var(--lumo-space-m)");
        gearGrid.setWidthFull();

        loadSentinel.setId("public-load-sentinel");
        loadSentinel.getStyle().set("height", "1px");
        loadSentinel.getStyle().set("width", "100%");
        loadSentinel.setVisible(false);
    }

    private List<GearItem> fetchNextBatch() {
        List<GearItem> items = gearItemService.listForUser(
                targetUserId, Optional.empty(), loadedCount, PAGE_SIZE);
        loadedCount += items.size();
        if (items.size() < PAGE_SIZE) {
            allLoaded = true;
        }
        return items;
    }

    private Component buildCard(GearItem item) {
        Div card = new Div();
        card.addClassName("public-gear-card");
        card.getElement().setAttribute("data-gear-item-id", String.valueOf(item.id()));
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        card.getStyle().set("padding", "var(--lumo-space-m)");

        H3 name = new H3();
        name.getStyle().set("margin", "0");
        name.getStyle().set("font-size", "var(--lumo-font-size-m)");

        // BR-002: no edit affordances. Catalog-linked items deep-link to the equipment page
        // (UC-007) per UC-013 step 5; free-text items render as plain text.
        if (item.equipmentId() != null) {
            Anchor link = new Anchor(EquipmentView.ROUTE + "/" + item.equipmentId(),
                    item.displayName());
            name.add(link);
        } else {
            name.setText(item.displayName());
        }
        card.add(name);

        String manufacturer = item.displayManufacturer();
        Span info = new Span(manufacturer == null ? "Not in catalog" : manufacturer);
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");
        info.getStyle().set("font-size", "var(--lumo-font-size-s)");
        info.getStyle().set("display", "block");

        Span category = new Span(item.category().label());
        category.getElement().getThemeList().add("badge");

        card.add(info, category);
        return card;
    }

    private void installInfiniteScroll() {
        loadSentinel.setVisible(true);
        // BR-003: lazy-load additional gear-item batches as the viewer approaches the end.
        getElement().executeJs(
                "const view = this;"
                        + "const sentinel = $0;"
                        + "if (view.__publicInventoryObserver) {"
                        + "  view.__publicInventoryObserver.disconnect();"
                        + "}"
                        + "view.__publicInventoryObserver = new IntersectionObserver((entries) => {"
                        + "  for (const entry of entries) {"
                        + "    if (entry.isIntersecting) {"
                        + "      view.$server.loadMore();"
                        + "    }"
                        + "  }"
                        + "}, { rootMargin: '200px' });"
                        + "view.__publicInventoryObserver.observe(sentinel);",
                loadSentinel.getElement());
    }

    private void disconnectInfiniteScroll() {
        loadSentinel.setVisible(false);
        getElement().executeJs(
                "if (this.__publicInventoryObserver) {"
                        + "  this.__publicInventoryObserver.disconnect();"
                        + "  this.__publicInventoryObserver = null;"
                        + "}");
    }

    @ClientCallable
    public void loadMore() {
        if (allLoaded || targetUserId == null) {
            return;
        }
        List<GearItem> items = fetchNextBatch();
        items.forEach(item -> gearGrid.add(buildCard(item)));
        if (allLoaded) {
            disconnectInfiniteScroll();
        }
    }
}
