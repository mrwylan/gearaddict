package app.gearaddict.views.auth;

import app.gearaddict.user.RegistrationException;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.UserService;
import app.gearaddict.views.inventory.InventoryView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route(value = "register", autoLayout = false)
@PageTitle("Create account — GearAddict")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public static final String ROUTE = "register";

    private final UserService userService;

    private final TextField usernameField = new TextField("Username");
    private final EmailField emailField = new EmailField("Email");
    private final PasswordField passwordField = new PasswordField("Password");
    private final Paragraph errorMessage = new Paragraph();

    public RegisterView(UserService userService) {
        this.userService = userService;

        addClassName("register-view");
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        usernameField.setId("username");
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setMaxLength(50);

        emailField.setId("email");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setErrorMessage("Enter a valid email address.");

        passwordField.setId("password");
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setHelperText("At least " + UserService.MIN_PASSWORD_LENGTH + " characters.");

        errorMessage.setId("error-message");
        errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
        errorMessage.setVisible(false);

        Button submit = new Button("Create account", e -> submit());
        submit.setId("submit");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button toLogin = new Button("Back to log in",
                e -> UI.getCurrent().navigate(LoginView.ROUTE));
        toLogin.setId("back-to-login");

        HorizontalLayout actions = new HorizontalLayout(submit, toLogin);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout form = new VerticalLayout(
                new H1("Create your GearAddict account"),
                usernameField,
                emailField,
                passwordField,
                errorMessage,
                actions);
        form.setMaxWidth("420px");
        form.setPadding(true);
        form.setSpacing(true);

        add(form);
    }

    private void submit() {
        errorMessage.setVisible(false);
        clearFieldErrors();

        String username = usernameField.getValue();
        String email = emailField.getValue();
        String password = passwordField.getValue();

        try {
            userService.register(new RegistrationRequest(username, email, password));
            authenticate(email);
            Notification.show("Welcome to GearAddict!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(InventoryView.ROUTE);
        } catch (RegistrationException ex) {
            handleRegistrationError(ex);
        }
    }

    private void handleRegistrationError(RegistrationException ex) {
        switch (ex.reason()) {
            case MISSING_FIELD -> {
                if (usernameField.isEmpty()) usernameField.setInvalid(true);
                if (emailField.isEmpty()) emailField.setInvalid(true);
                if (passwordField.isEmpty()) passwordField.setInvalid(true);
                showError("Please fill in all required fields.");
            }
            case INVALID_EMAIL -> {
                emailField.setInvalid(true);
                showError("Enter a valid email address.");
            }
            case PASSWORD_TOO_SHORT -> {
                passwordField.setInvalid(true);
                passwordField.setErrorMessage(
                        "Password must be at least " + UserService.MIN_PASSWORD_LENGTH + " characters.");
                showError("Password does not meet the requirements.");
            }
            case USERNAME_TAKEN -> {
                usernameField.setInvalid(true);
                usernameField.setErrorMessage("Username is already taken.");
                showError("That username is unavailable.");
            }
            case EMAIL_TAKEN -> {
                emailField.setInvalid(true);
                emailField.setErrorMessage("Email is already registered.");
                showError("That email is already in use.");
            }
        }
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
    }

    private void clearFieldErrors() {
        usernameField.setInvalid(false);
        emailField.setInvalid(false);
        passwordField.setInvalid(false);
    }

    private void authenticate(String principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
