package app.gearaddict.views.connections;

import app.gearaddict.connection.GearConnection;
import app.gearaddict.connection.GearConnectionService;
import app.gearaddict.connection.SharedEquipment;
import app.gearaddict.user.User;
import app.gearaddict.user.UserService;
import app.gearaddict.views.MainLayout;
import app.gearaddict.views.profile.PublicProfileView;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "connections", layout = MainLayout.class)
@PageTitle("Gear Connections — GearAddict")
@PermitAll
public class ConnectionsView extends VerticalLayout {

    public static final String ROUTE = "connections";
    static final int PAGE_SIZE = 20;

    private final transient AuthenticationContext authenticationContext;
    private final transient UserService userService;
    private final transient GearConnectionService gearConnectionService;

    private final Div connectionList = new Div();
    private final Div loadSentinel = new Div();
    private final Div emptyState = new Div();

    private Long currentUserId;
    private int loadedCount;
    private boolean allLoaded;

    public ConnectionsView(AuthenticationContext authenticationContext,
                           UserService userService,
                           GearConnectionService gearConnectionService) {
        this.authenticationContext = authenticationContext;
        this.userService = userService;
        this.gearConnectionService = gearConnectionService;

        addClassName("connections-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Gear Connections");
        title.getStyle().set("margin", "0");
        add(title);

        configureContainers();
        add(connectionList, loadSentinel, emptyState);

        currentUserId = loadCurrentUser().map(User::id).orElse(null);
        refresh();
    }

    private void configureContainers() {
        connectionList.setId("connection-list");
        connectionList.getStyle().set("display", "flex");
        connectionList.getStyle().set("flex-direction", "column");
        connectionList.getStyle().set("gap", "var(--lumo-space-m)");
        connectionList.setWidthFull();

        loadSentinel.setId("connections-load-sentinel");
        loadSentinel.getStyle().set("height", "1px");
        loadSentinel.getStyle().set("width", "100%");
        loadSentinel.setVisible(false);

        emptyState.setId("connections-empty-state");
        emptyState.getStyle().set("color", "var(--lumo-secondary-text-color)");
        emptyState.getStyle().set("padding", "var(--lumo-space-l)");
        emptyState.setVisible(false);
    }

    private void refresh() {
        connectionList.removeAll();
        emptyState.removeAll();
        emptyState.setVisible(false);
        loadSentinel.setVisible(false);
        loadedCount = 0;
        allLoaded = false;

        if (currentUserId == null) {
            showEmpty(new Paragraph("You are not signed in."));
            return;
        }

        if (!gearConnectionService.hasCatalogLinkedItems(currentUserId)) {
            // A2: inventory has no catalog-linked items.
            Paragraph note = new Paragraph(
                    "Connections are only available for catalog-linked gear items. "
                            + "Add gear from the catalog to discover other owners.");
            note.setId("no-catalog-items");
            showEmpty(note);
            return;
        }

        List<GearConnection> firstBatch = fetchNextBatch();
        if (firstBatch.isEmpty()) {
            // A1: no other public users own any of the same equipment.
            Paragraph note = new Paragraph(
                    "No connections yet. As more people add the same gear to their public "
                            + "inventories, they'll appear here.");
            note.setId("no-connections");
            showEmpty(note);
            return;
        }

        connectionList.setVisible(true);
        firstBatch.forEach(connection -> connectionList.add(buildRow(connection)));
        if (!allLoaded) {
            installInfiniteScroll();
        }
    }

    private List<GearConnection> fetchNextBatch() {
        List<GearConnection> connections = gearConnectionService.findConnections(
                currentUserId, loadedCount, PAGE_SIZE);
        loadedCount += connections.size();
        if (connections.size() < PAGE_SIZE) {
            allLoaded = true;
        }
        return connections;
    }

    private Component buildRow(GearConnection connection) {
        Div row = new Div();
        row.addClassName("connection-row");
        row.getElement().setAttribute("data-connection-user-id", String.valueOf(connection.userId()));
        row.getStyle().set("background", "var(--lumo-base-color)");
        row.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        row.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        row.getStyle().set("padding", "var(--lumo-space-m)");

        Anchor profileLink = new Anchor(
                PublicProfileView.ROUTE + "/" + connection.userId(),
                connection.username());
        profileLink.setId("connection-username-" + connection.userId());
        profileLink.getStyle().set("font-weight", "600");
        profileLink.getStyle().set("font-size", "var(--lumo-font-size-l)");

        H3 nameWrap = new H3();
        nameWrap.getStyle().set("margin", "0");
        nameWrap.add(profileLink);

        String summary = "Shares " + summarizeShared(connection.sharedEquipment());
        Span sharedSummary = new Span(summary);
        sharedSummary.addClassName("connection-shared");
        sharedSummary.getStyle().set("color", "var(--lumo-secondary-text-color)");
        sharedSummary.getStyle().set("display", "block");
        sharedSummary.getStyle().set("margin-top", "var(--lumo-space-xs)");

        row.add(nameWrap, sharedSummary);
        return row;
    }

    private static String summarizeShared(List<SharedEquipment> shared) {
        if (shared.size() == 1) {
            return shared.get(0).name();
        }
        return shared.stream().map(SharedEquipment::name).collect(Collectors.joining(", "));
    }

    private void showEmpty(Component message) {
        connectionList.setVisible(false);
        emptyState.setVisible(true);
        emptyState.add(message);
    }

    private void installInfiniteScroll() {
        loadSentinel.setVisible(true);
        // BR-004: lazy-load additional connection batches as the user nears the bottom.
        getElement().executeJs(
                "const view = this;"
                        + "const sentinel = $0;"
                        + "if (view.__connectionsObserver) {"
                        + "  view.__connectionsObserver.disconnect();"
                        + "}"
                        + "view.__connectionsObserver = new IntersectionObserver((entries) => {"
                        + "  for (const entry of entries) {"
                        + "    if (entry.isIntersecting) {"
                        + "      view.$server.loadMore();"
                        + "    }"
                        + "  }"
                        + "}, { rootMargin: '200px' });"
                        + "view.__connectionsObserver.observe(sentinel);",
                loadSentinel.getElement());
    }

    private void disconnectInfiniteScroll() {
        loadSentinel.setVisible(false);
        getElement().executeJs(
                "if (this.__connectionsObserver) {"
                        + "  this.__connectionsObserver.disconnect();"
                        + "  this.__connectionsObserver = null;"
                        + "}");
    }

    @ClientCallable
    public void loadMore() {
        if (allLoaded || currentUserId == null) {
            return;
        }
        List<GearConnection> connections = fetchNextBatch();
        connections.forEach(connection -> connectionList.add(buildRow(connection)));
        if (allLoaded) {
            disconnectInfiniteScroll();
        }
    }

    private Optional<User> loadCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(principal -> userService.findByEmail(principal.getUsername()));
    }
}
