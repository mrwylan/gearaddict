package app.gearaddict.views.equipment;

import app.gearaddict.discussion.DiscussionThreadService;
import app.gearaddict.discussion.DiscussionThreadSummary;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.user.User;
import app.gearaddict.user.UserService;
import app.gearaddict.views.MainLayout;
import app.gearaddict.views.discussions.DiscussionsView;
import app.gearaddict.views.thread.ThreadView;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Route(value = "equipment", layout = MainLayout.class)
@PageTitle("Equipment — GearAddict")
@AnonymousAllowed
public class EquipmentView extends VerticalLayout implements HasUrlParameter<Long> {

    public static final String ROUTE = "equipment";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final transient EquipmentService equipmentService;
    private final transient DiscussionThreadService discussionThreadService;
    private final transient UserService userService;
    private final transient AuthenticationContext authenticationContext;

    public EquipmentView(EquipmentService equipmentService,
                         DiscussionThreadService discussionThreadService,
                         UserService userService,
                         AuthenticationContext authenticationContext) {
        this.equipmentService = equipmentService;
        this.discussionThreadService = discussionThreadService;
        this.userService = userService;
        this.authenticationContext = authenticationContext;

        addClassName("equipment-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long equipmentId) {
        removeAll();

        if (equipmentId == null) {
            throw new NotFoundException();
        }

        Optional<Equipment> equipment = equipmentService.findById(equipmentId);
        if (equipment.isEmpty()) {
            renderNotFound();
            return;
        }

        render(equipment.get());
    }

    private void renderNotFound() {
        H1 title = new H1("Equipment not found");
        title.setId("equipment-not-found");
        Paragraph message = new Paragraph(
                "The equipment you are looking for does not exist or has been removed.");
        RouterLink backToCatalog = new RouterLink("Back to catalog",
                app.gearaddict.views.catalog.CatalogView.class);
        backToCatalog.setId("back-to-catalog");
        add(title, message, backToCatalog);
    }

    private void render(Equipment equipment) {
        add(buildHeader(equipment));
        add(buildSpecifications(equipment));
        add(buildOwnerCount(equipment));
        add(buildDiscussionSummary(equipment));
    }

    private VerticalLayout buildHeader(Equipment equipment) {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);

        H1 title = new H1(equipment.name());
        title.setId("equipment-name");

        Span manufacturer = new Span(equipment.manufacturer());
        manufacturer.setId("equipment-manufacturer");
        manufacturer.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span category = new Span(equipment.category().label());
        category.setId("equipment-category");
        category.getElement().getThemeList().add("badge");

        HorizontalLayout meta = new HorizontalLayout(manufacturer,
                new Span("·"), category);
        meta.setSpacing(true);

        header.add(title, meta);
        return header;
    }

    private VerticalLayout buildSpecifications(Equipment equipment) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        H2 heading = new H2("Specifications");
        Paragraph description = new Paragraph(
                equipment.description() == null || equipment.description().isBlank()
                        ? "No technical specifications available."
                        : equipment.description());
        description.setId("equipment-description");

        section.add(heading, description);
        return section;
    }

    private VerticalLayout buildOwnerCount(Equipment equipment) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        H2 heading = new H2("Community owners");
        int owners = equipmentService.countOwners(equipment.id());
        Paragraph text = new Paragraph();
        text.setId("equipment-owner-count");
        text.add(new Text(owners == 1
                ? "1 user has added this item to their inventory."
                : owners + " users have added this item to their inventory."));

        section.add(heading, text);
        return section;
    }

    private VerticalLayout buildDiscussionSummary(Equipment equipment) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setId("equipment-discussions");

        H2 heading = new H2("Discussion threads");

        List<DiscussionThreadSummary> threads = discussionThreadService
                .recentForEquipment(equipment.id(), DiscussionThreadService.EQUIPMENT_PAGE_SUMMARY_LIMIT);
        int totalThreads = discussionThreadService.countForEquipment(equipment.id());

        HorizontalLayout headingRow = new HorizontalLayout(heading);
        headingRow.setWidthFull();
        headingRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        headingRow.setJustifyContentMode(
                com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        currentUser().ifPresent(user -> headingRow.add(buildStartDiscussionButton(equipment, user)));
        section.add(headingRow);

        if (threads.isEmpty()) {
            Paragraph empty = new Paragraph("No discussions started yet.");
            empty.setId("equipment-discussions-empty");
            section.add(empty);
            return section;
        }

        Div list = new Div();
        list.setId("equipment-discussion-list");
        for (DiscussionThreadSummary thread : threads) {
            list.add(buildThreadItem(thread));
        }
        section.add(list);

        if (totalThreads > threads.size()) {
            RouterLink viewAll = new RouterLink(
                    "View all " + totalThreads + " discussions",
                    DiscussionsView.class);
            viewAll.setId("view-all-discussions");
            section.add(viewAll);
        }

        return section;
    }

    private Button buildStartDiscussionButton(Equipment equipment, User user) {
        Button button = new Button("Start a Discussion", VaadinIcon.PLUS.create());
        button.setId("start-discussion");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(e -> new NewThreadDialog(
                discussionThreadService, equipment.id(), user.id()).open());
        return button;
    }

    private java.util.Optional<User> currentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(principal -> userService.findByEmail(principal.getUsername()));
    }

    private Div buildThreadItem(DiscussionThreadSummary thread) {
        Div item = new Div();
        item.getStyle().set("padding", "var(--lumo-space-s) 0");
        item.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Anchor titleLink = new Anchor(ThreadView.ROUTE + "/" + thread.id(), thread.title());
        titleLink.getStyle().set("font-weight", "600");

        Span author = new Span("by " + thread.authorUsername());
        author.getStyle().set("color", "var(--lumo-secondary-text-color)");
        author.getStyle().set("margin-left", "var(--lumo-space-s)");

        Span timestamp = new Span(DATE_FORMATTER.format(
                thread.lastReplyAt() == null ? thread.createdAt() : thread.lastReplyAt()));
        timestamp.getStyle().set("color", "var(--lumo-secondary-text-color)");
        timestamp.getStyle().set("margin-left", "var(--lumo-space-s)");

        Span replies = new Span(thread.replyCount()
                + (thread.replyCount() == 1 ? " reply" : " replies"));
        replies.getStyle().set("color", "var(--lumo-secondary-text-color)");
        replies.getStyle().set("margin-left", "var(--lumo-space-s)");

        item.add(titleLink, author, timestamp, replies);
        return item;
    }
}
