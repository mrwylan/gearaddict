package app.gearaddict.views.equipment;

import app.gearaddict.discussion.DiscussionThreadException;
import app.gearaddict.discussion.DiscussionThreadService;
import app.gearaddict.views.thread.ThreadView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

class NewThreadDialog extends Dialog {

    private final transient DiscussionThreadService discussionThreadService;
    private final Long equipmentId;
    private final Long authorId;

    private final TextField titleField = new TextField("Title");
    private final TextArea bodyField = new TextArea("Opening post");

    NewThreadDialog(DiscussionThreadService discussionThreadService,
                    Long equipmentId,
                    Long authorId) {
        this.discussionThreadService = discussionThreadService;
        this.equipmentId = equipmentId;
        this.authorId = authorId;

        setId("new-thread-dialog");
        setHeaderTitle("Start a Discussion");
        setWidth("520px");

        titleField.setId("new-thread-title");
        titleField.setRequiredIndicatorVisible(true);
        titleField.setMaxLength(DiscussionThreadService.MAX_TITLE_LENGTH);
        titleField.setHelperText(DiscussionThreadService.MIN_TITLE_LENGTH
                + "–" + DiscussionThreadService.MAX_TITLE_LENGTH + " characters.");
        titleField.setWidthFull();

        bodyField.setId("new-thread-body");
        bodyField.setRequiredIndicatorVisible(true);
        bodyField.setMaxLength(DiscussionThreadService.MAX_BODY_LENGTH);
        bodyField.setHelperText("At least " + DiscussionThreadService.MIN_BODY_LENGTH
                + " characters; up to " + DiscussionThreadService.MAX_BODY_LENGTH + ".");
        bodyField.setWidthFull();
        bodyField.setMinHeight("180px");

        VerticalLayout form = new VerticalLayout(titleField, bodyField);
        form.setPadding(false);
        form.setSpacing(true);
        add(form);

        Button save = new Button("Post thread", e -> save());
        save.setId("new-thread-save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> close());
        cancel.setId("new-thread-cancel");

        getFooter().add(new HorizontalLayout(cancel, save));
    }

    private void save() {
        clearFieldErrors();
        try {
            Long threadId = discussionThreadService.startThread(
                    equipmentId, authorId, titleField.getValue(), bodyField.getValue());
            close();
            Notification.show("Discussion started.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(ThreadView.class, threadId);
        } catch (DiscussionThreadException ex) {
            handleError(ex);
        }
    }

    private void handleError(DiscussionThreadException ex) {
        switch (ex.reason()) {
            case TITLE_REQUIRED, TITLE_TOO_SHORT, TITLE_TOO_LONG -> {
                titleField.setInvalid(true);
                titleField.setErrorMessage(ex.getMessage());
            }
            case BODY_REQUIRED, BODY_TOO_SHORT, BODY_TOO_LONG -> {
                bodyField.setInvalid(true);
                bodyField.setErrorMessage(ex.getMessage());
            }
            case EQUIPMENT_NOT_FOUND, AUTHOR_NOT_FOUND -> Notification.show(ex.getMessage(),
                            4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void clearFieldErrors() {
        titleField.setInvalid(false);
        bodyField.setInvalid(false);
    }
}
