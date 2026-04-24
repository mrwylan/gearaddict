package app.gearaddict.views.thread;

import app.gearaddict.discussion.DiscussionReply;
import app.gearaddict.discussion.DiscussionThreadDetail;
import app.gearaddict.discussion.DiscussionThreadService;
import app.gearaddict.views.MainLayout;
import app.gearaddict.views.discussions.DiscussionsView;
import app.gearaddict.views.equipment.EquipmentView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Thread detail view (UC-008): shows the opening post plus all replies in
 * chronological order with infinite scroll (BR-002, BR-004). Accessible to
 * unauthenticated visitors (BR-001). Not a top-level nav entry; reached from
 * DiscussionsView or the equipment page.
 */
@Route(value = "thread", layout = MainLayout.class)
@PageTitle("Discussion — GearAddict")
@AnonymousAllowed
public class ThreadView extends VerticalLayout implements HasUrlParameter<Long> {

    public static final String ROUTE = "thread";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final transient DiscussionThreadService discussionThreadService;

    public ThreadView(DiscussionThreadService discussionThreadService) {
        this.discussionThreadService = discussionThreadService;

        addClassName("thread-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long threadId) {
        removeAll();

        if (threadId == null) {
            renderNotFound();
            return;
        }

        Optional<DiscussionThreadDetail> detail = discussionThreadService.findDetail(threadId);
        if (detail.isEmpty()) {
            renderNotFound();
            return;
        }

        render(detail.get());
    }

    private void renderNotFound() {
        H1 title = new H1("Thread not found");
        title.setId("thread-not-found");
        Paragraph message = new Paragraph(
                "The discussion thread you are looking for does not exist or has been removed.");
        RouterLink back = new RouterLink("Back to discussions", DiscussionsView.class);
        back.setId("back-to-discussions");
        add(title, message, back);
    }

    private void render(DiscussionThreadDetail thread) {
        add(buildBreadcrumbs(thread));
        add(buildOpeningPost(thread));
        add(buildRepliesSection(thread));
    }

    private Component buildBreadcrumbs(DiscussionThreadDetail thread) {
        RouterLink discussionsLink = new RouterLink("Discussions", DiscussionsView.class);
        discussionsLink.setId("breadcrumb-discussions");

        RouterLink equipmentLink = new RouterLink(
                thread.equipmentManufacturer() + " · " + thread.equipmentName(),
                EquipmentView.class,
                thread.equipmentId());
        equipmentLink.setId("breadcrumb-equipment");

        HorizontalLayout breadcrumbs = new HorizontalLayout(
                discussionsLink, new Span("›"), equipmentLink);
        breadcrumbs.setSpacing(true);
        breadcrumbs.setPadding(false);
        breadcrumbs.getStyle().set("color", "var(--lumo-secondary-text-color)");
        breadcrumbs.getStyle().set("font-size", "var(--lumo-font-size-s)");
        return breadcrumbs;
    }

    private Component buildOpeningPost(DiscussionThreadDetail thread) {
        VerticalLayout section = new VerticalLayout();
        section.setId("opening-post");
        section.setPadding(false);
        section.setSpacing(false);

        H1 title = new H1(thread.title());
        title.setId("thread-title");
        title.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Span author = new Span("by " + thread.authorUsername());
        author.setId("thread-author");
        author.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span timestamp = new Span(TIMESTAMP_FORMATTER.format(thread.createdAt()));
        timestamp.setId("thread-created-at");
        timestamp.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout meta = new HorizontalLayout(author, new Span("·"), timestamp);
        meta.setSpacing(true);
        meta.setPadding(false);
        meta.getStyle().set("font-size", "var(--lumo-font-size-s)");
        meta.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        Paragraph body = new Paragraph(thread.body());
        body.setId("thread-body");
        body.getStyle().set("white-space", "pre-wrap");

        section.add(title, meta, body);
        return section;
    }

    private Component buildRepliesSection(DiscussionThreadDetail thread) {
        VerticalLayout section = new VerticalLayout();
        section.setId("replies-section");
        section.setPadding(false);
        section.setSpacing(false);
        section.setSizeFull();

        int replyCount = discussionThreadService.countReplies(thread.id());

        H3 heading = new H3(replyCount == 0
                ? "Replies"
                : "Replies (" + replyCount + ")");
        heading.getStyle().set("margin-top", "var(--lumo-space-l)");
        section.add(heading);

        if (replyCount == 0) {
            Paragraph empty = new Paragraph("No replies yet.");
            empty.setId("replies-empty");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            section.add(empty);
            return section;
        }

        VirtualList<DiscussionReply> replies = new VirtualList<>();
        replies.setId("reply-list");
        replies.setSizeFull();
        replies.setRenderer(new ComponentRenderer<>(this::buildReplyRow));
        replies.setDataProvider(DataProvider.fromCallbacks(
                query -> discussionThreadService
                        .listReplies(thread.id(), query.getOffset(), query.getLimit())
                        .stream(),
                query -> discussionThreadService.countReplies(thread.id())));

        section.add(replies);
        section.expand(replies);
        return section;
    }

    private Component buildReplyRow(DiscussionReply reply) {
        Div row = new Div();
        row.addClassName("reply-row");
        row.getElement().setAttribute("data-reply-id", String.valueOf(reply.id()));
        row.getStyle().set("padding", "var(--lumo-space-m) 0");
        row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Span author = new Span(reply.authorUsername());
        author.addClassName("reply-author");
        author.getStyle().set("font-weight", "600");

        Span timestamp = new Span(TIMESTAMP_FORMATTER.format(reply.createdAt()));
        timestamp.addClassName("reply-timestamp");
        timestamp.getStyle().set("color", "var(--lumo-secondary-text-color)");
        timestamp.getStyle().set("margin-left", "var(--lumo-space-s)");
        timestamp.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Paragraph body = new Paragraph(reply.body());
        body.addClassName("reply-body");
        body.getStyle().set("white-space", "pre-wrap");
        body.getStyle().set("margin", "var(--lumo-space-xs) 0 0 0");

        Div header = new Div(author, timestamp);
        row.add(header, body);
        return row;
    }
}
