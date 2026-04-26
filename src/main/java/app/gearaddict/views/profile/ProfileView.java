package app.gearaddict.views.profile;

import app.gearaddict.user.ProfileUpdateException;
import app.gearaddict.user.User;
import app.gearaddict.user.UserService;
import app.gearaddict.views.MainLayout;
import app.gearaddict.views.inventory.InventoryView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile — GearAddict")
@PermitAll
public class ProfileView extends VerticalLayout {

    public static final String ROUTE = "profile";

    private final transient UserService userService;
    private final transient AuthenticationContext authenticationContext;

    private final TextField emailField = new TextField("Email");
    private final TextField usernameField = new TextField("Display name");
    private final TextArea bioField = new TextArea("Bio");
    private final Checkbox publicInventoryToggle = new Checkbox("Make my inventory publicly visible");
    private final Paragraph errorMessage = new Paragraph();
    private Long userId;
    private String originalUsername = "";
    private String originalBio = "";
    private boolean originalPublicInventory;

    public ProfileView(UserService userService, AuthenticationContext authenticationContext) {
        this.userService = userService;
        this.authenticationContext = authenticationContext;

        addClassName("profile-view");
        setSizeFull();
        setPadding(true);

        emailField.setId("email");
        emailField.setReadOnly(true);
        emailField.setHelperText("Used to sign in. Contact support to change it.");

        usernameField.setId("username");
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setMaxLength(UserService.MAX_USERNAME_LENGTH);
        usernameField.setHelperText("1–" + UserService.MAX_USERNAME_LENGTH + " characters.");

        bioField.setId("bio");
        bioField.setMaxLength(UserService.MAX_BIO_LENGTH);
        bioField.setHelperText("Up to " + UserService.MAX_BIO_LENGTH + " characters.");
        bioField.setWidthFull();

        publicInventoryToggle.setId("public-inventory-toggle");
        publicInventoryToggle.setHelperText(
                "When enabled, other signed-in users can see your inventory and connect with you.");

        errorMessage.setId("error-message");
        errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
        errorMessage.setVisible(false);

        Button save = new Button("Save changes", e -> save());
        save.setId("save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> cancel());
        cancel.setId("cancel");

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout form = new VerticalLayout(
                new H1("Profile"),
                emailField,
                usernameField,
                bioField,
                publicInventoryToggle,
                errorMessage,
                actions);
        form.setMaxWidth("640px");
        form.setPadding(false);
        form.setSpacing(true);

        add(form);

        loadCurrentUser().ifPresentOrElse(user -> {
            userId = user.id();
            originalUsername = user.username();
            originalBio = user.bio() == null ? "" : user.bio();
            originalPublicInventory = user.publicInventory();
            emailField.setValue(user.email() == null ? "" : user.email());
            usernameField.setValue(originalUsername);
            bioField.setValue(originalBio);
            publicInventoryToggle.setValue(originalPublicInventory);
        }, () -> {
            save.setEnabled(false);
            showError("You are not signed in.");
        });
    }

    private Optional<User> loadCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(principal -> userService.findByEmail(principal.getUsername()));
    }

    private void save() {
        errorMessage.setVisible(false);
        clearFieldErrors();

        try {
            userService.updateProfile(userId, usernameField.getValue(), bioField.getValue());
            boolean newVisibility = Boolean.TRUE.equals(publicInventoryToggle.getValue());
            if (newVisibility != originalPublicInventory) {
                userService.setInventoryVisibility(userId, newVisibility);
                originalPublicInventory = newVisibility;
            }
            originalUsername = usernameField.getValue().trim();
            originalBio = bioField.getValue() == null ? "" : bioField.getValue().trim();
            Notification.show("Profile updated.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (ProfileUpdateException ex) {
            handleError(ex);
        }
    }

    private void cancel() {
        errorMessage.setVisible(false);
        clearFieldErrors();
        usernameField.setValue(originalUsername);
        bioField.setValue(originalBio);
        publicInventoryToggle.setValue(originalPublicInventory);
        UI.getCurrent().navigate(InventoryView.ROUTE);
    }

    private void handleError(ProfileUpdateException ex) {
        switch (ex.reason()) {
            case USERNAME_EMPTY -> {
                usernameField.setInvalid(true);
                usernameField.setErrorMessage("Username is required.");
                showError("Please enter a display name.");
            }
            case USERNAME_TOO_LONG -> {
                usernameField.setInvalid(true);
                usernameField.setErrorMessage(
                        "Must be at most " + UserService.MAX_USERNAME_LENGTH + " characters.");
                showError("Display name is too long.");
            }
            case USERNAME_TAKEN -> {
                usernameField.setInvalid(true);
                usernameField.setErrorMessage("Username is already taken.");
                showError("That display name is unavailable.");
            }
            case BIO_TOO_LONG -> {
                bioField.setInvalid(true);
                bioField.setErrorMessage(
                        "Must be at most " + UserService.MAX_BIO_LENGTH + " characters.");
                showError("Bio is too long.");
            }
            case USER_NOT_FOUND -> showError("Your account could not be found.");
        }
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
    }

    private void clearFieldErrors() {
        usernameField.setInvalid(false);
        bioField.setInvalid(false);
    }
}
