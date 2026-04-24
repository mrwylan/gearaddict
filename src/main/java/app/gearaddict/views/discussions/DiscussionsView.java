package app.gearaddict.views.discussions;

import app.gearaddict.discussion.DiscussionThreadListItem;
import app.gearaddict.discussion.DiscussionThreadService;
import app.gearaddict.views.MainLayout;
import app.gearaddict.views.thread.ThreadView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.time.format.DateTimeFormatter;

/**
 * Top-level Discussions view (FR-011 / UC-008): lists threads aggregated across
 * the catalog, most recently active first, with infinite scroll (BR-004).
 */
@Route(value = "discussions", layout = MainLayout.class)
@PageTitle("Discussions — GearAddict")
@AnonymousAllowed
public class DiscussionsView extends VerticalLayout {

    public static final String ROUTE = "discussions";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final transient DiscussionThreadService discussionThreadService;
    private final VirtualList<DiscussionThreadListItem> list = new VirtualList<>();
    private final Div emptyState = new Div();

    public DiscussionsView(DiscussionThreadService discussionThreadService) {
        this.discussionThreadService = discussionThreadService;

        addClassName("discussions-view");
        setSizeFull();
        setPadding(true);

        H1 heading = new H1("Discussions");
        heading.setId("discussions-heading");

        Paragraph subtitle = new Paragraph(
                "Community conversations across all equipment, most recently active first.");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        subtitle.getStyle().set("margin-top", "0");

        configureList();
        configureEmptyState();

        add(heading, subtitle, list, emptyState);
        expand(list);

        refresh();
    }

    private void configureList() {
        list.setId("discussion-thread-list");
        list.setSizeFull();
        list.setRenderer(new ComponentRenderer<>(this::buildThreadRow));
    }

    private void configureEmptyState() {
        emptyState.setId("discussions-empty");
        emptyState.getStyle().set("text-align", "center");
        emptyState.getStyle().set("padding", "var(--lumo-space-xl)");
        emptyState.getStyle().set("color", "var(--lumo-secondary-text-color)");
        emptyState.setVisible(false);
    }

    private void refresh() {
        int total = discussionThreadService.countAll();
        if (total == 0) {
            list.setVisible(false);
            emptyState.removeAll();
            emptyState.add(new Paragraph(
                    "No discussions yet. Be the first to start one from an equipment page."));
            emptyState.setVisible(true);
            return;
        }
        list.setVisible(true);
        emptyState.setVisible(false);
        list.setDataProvider(DataProvider.fromCallbacks(
                query -> discussionThreadService
                        .listAggregated(query.getOffset(), query.getLimit())
                        .stream(),
                query -> discussionThreadService.countAll()));
    }

    private Component buildThreadRow(DiscussionThreadListItem thread) {
        Div row = new Div();
        row.addClassName("discussion-row");
        row.getStyle().set("display", "flex");
        row.getStyle().set("flex-direction", "column");
        row.getStyle().set("gap", "var(--lumo-space-xs)");
        row.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-s)");
        row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        row.getStyle().set("cursor", "pointer");
        row.getElement().setAttribute("data-thread-id", String.valueOf(thread.id()));

        Anchor titleLink = new Anchor(ThreadView.ROUTE + "/" + thread.id(), thread.title());
        titleLink.addClassName("discussion-title");
        titleLink.getStyle().set("font-weight", "600");
        titleLink.getStyle().set("font-size", "var(--lumo-font-size-l)");
        titleLink.getStyle().set("color", "var(--lumo-primary-text-color)");

        Span equipment = new Span(thread.equipmentManufacturer() + " · " + thread.equipmentName());
        equipment.addClassName("discussion-equipment");
        equipment.getStyle().set("color", "var(--lumo-secondary-text-color)");
        equipment.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Span author = new Span("by " + thread.authorUsername());
        author.addClassName("discussion-author");
        author.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span date = new Span(DATE_FORMATTER.format(
                thread.lastReplyAt() == null ? thread.createdAt() : thread.lastReplyAt()));
        date.addClassName("discussion-date");
        date.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span replyCountLabel = new Span(" " + thread.replyCount()
                + (thread.replyCount() == 1 ? " reply" : " replies"));
        HorizontalLayout replyCount = new HorizontalLayout(VaadinIcon.COMMENT.create(), replyCountLabel);
        replyCount.addClassName("discussion-reply-count");
        replyCount.setSpacing(false);
        replyCount.setPadding(false);
        replyCount.setAlignItems(FlexComponent.Alignment.CENTER);
        replyCount.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout meta = new HorizontalLayout(author, new Span("·"), date, new Span("·"), replyCount);
        meta.setSpacing(true);
        meta.setPadding(false);
        meta.setAlignItems(FlexComponent.Alignment.CENTER);
        meta.getStyle().set("font-size", "var(--lumo-font-size-s)");

        row.add(titleLink, equipment, meta);
        row.getElement().addEventListener("click",
                e -> UI.getCurrent().navigate(ThreadView.class, thread.id()));
        return row;
    }
}
