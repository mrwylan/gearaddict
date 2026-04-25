package app.gearaddict.views.thread;

import app.gearaddict.discussion.DiscussionReply;
import app.gearaddict.discussion.DiscussionThreadDetail;
import app.gearaddict.discussion.DiscussionThreadService;
import app.gearaddict.discussion.ReplyException;
import app.gearaddict.user.User;
import app.gearaddict.user.UserService;
import app.gearaddict.views.MainLayout;
import app.gearaddict.views.discussions.DiscussionsView;
import app.gearaddict.views.equipment.EquipmentView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;

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
    private final transient UserService userService;
    private final transient AuthenticationContext authenticationContext;

    private Long currentThreadId;
    private VirtualList<DiscussionReply> replyList;
    private H3 repliesHeading;
    private Paragraph repliesEmpty;
    private VerticalLayout repliesSection;

    public ThreadView(DiscussionThreadService discussionThreadService,
                      UserService userService,
                      AuthenticationContext authenticationContext) {
        this.discussionThreadService = discussionThreadService;
        this.userService = userService;
        this.authenticationContext = authenticationContext;

        addClassName("thread-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long threadId) {
        removeAll();
        currentThreadId = null;
        replyList = null;
        repliesHeading = null;
        repliesEmpty = null;
        repliesSection = null;

        if (threadId == null) {
            renderNotFound();
            return;
        }

        Optional<DiscussionThreadDetail> detail = discussionThreadService.findDetail(threadId);
        if (detail.isEmpty()) {
            renderNotFound();
            return;
        }

        currentThreadId = threadId;
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
        currentUser().ifPresent(user -> add(buildReplyForm(thread, user)));
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
        repliesSection = new VerticalLayout();
        repliesSection.setId("replies-section");
        repliesSection.setPadding(false);
        repliesSection.setSpacing(false);
        repliesSection.setSizeFull();

        int replyCount = discussionThreadService.countReplies(thread.id());

        repliesHeading = new H3(replyHeadingText(replyCount));
        repliesHeading.setId("replies-heading");
        repliesHeading.getStyle().set("margin-top", "var(--lumo-space-l)");
        repliesSection.add(repliesHeading);

        repliesEmpty = new Paragraph("No replies yet.");
        repliesEmpty.setId("replies-empty");
        repliesEmpty.getStyle().set("color", "var(--lumo-secondary-text-color)");
        repliesEmpty.setVisible(replyCount == 0);
        repliesSection.add(repliesEmpty);

        replyList = new VirtualList<>();
        replyList.setId("reply-list");
        replyList.setSizeFull();
        replyList.setRenderer(new ComponentRenderer<>(this::buildReplyRow));
        replyList.setDataProvider(DataProvider.fromCallbacks(
                query -> discussionThreadService
                        .listReplies(thread.id(), query.getOffset(), query.getLimit())
                        .stream(),
                query -> discussionThreadService.countReplies(thread.id())));
        replyList.setVisible(replyCount > 0);
        repliesSection.add(replyList);
        repliesSection.expand(replyList);
        return repliesSection;
    }

    private static String replyHeadingText(int replyCount) {
        return replyCount == 0 ? "Replies" : "Replies (" + replyCount + ")";
    }

    private Component buildReplyForm(DiscussionThreadDetail thread, User author) {
        VerticalLayout form = new VerticalLayout();
        form.setId("reply-form");
        form.setPadding(false);
        form.setSpacing(true);

        H3 heading = new H3("Reply");
        heading.getStyle().set("margin-top", "var(--lumo-space-l)");

        TextArea body = new TextArea();
        body.setId("reply-body");
        body.setPlaceholder("Share your thoughts…");
        body.setMaxLength(DiscussionThreadService.MAX_REPLY_LENGTH);
        body.setHelperText("Up to " + DiscussionThreadService.MAX_REPLY_LENGTH + " characters.");
        body.setWidthFull();
        body.setMinHeight("120px");

        Button submit = new Button("Post reply", e -> postReply(thread.id(), author.id(), body));
        submit.setId("reply-submit");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        form.add(heading, body, submit);
        return form;
    }

    private void postReply(Long threadId, Long authorId, TextArea body) {
        body.setInvalid(false);
        try {
            discussionThreadService.reply(threadId, authorId, body.getValue());
            body.clear();
            refreshReplies();
            Notification.show("Reply posted.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (ReplyException ex) {
            switch (ex.reason()) {
                case BODY_REQUIRED, BODY_TOO_LONG -> {
                    body.setInvalid(true);
                    body.setErrorMessage(ex.getMessage());
                }
                case THREAD_NOT_FOUND, AUTHOR_NOT_FOUND -> Notification.show(ex.getMessage(),
                                4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private void refreshReplies() {
        if (currentThreadId == null) {
            return;
        }
        int replyCount = discussionThreadService.countReplies(currentThreadId);
        repliesHeading.setText(replyHeadingText(replyCount));
        repliesEmpty.setVisible(replyCount == 0);
        replyList.setVisible(replyCount > 0);
        replyList.getDataProvider().refreshAll();
    }

    private Optional<User> currentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(principal -> userService.findByEmail(principal.getUsername()));
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
