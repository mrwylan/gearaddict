package app.gearaddict.views.auth;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "login", autoLayout = false)
@PageTitle("Log in — GearAddict")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    public static final String ROUTE = "login";

    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);

        Button register = new Button("Create account");
        register.setId("register-link");
        register.addClickShortcut(Key.KEY_R);
        register.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(RegisterView.ROUTE)));

        VerticalLayout links = new VerticalLayout(new Paragraph("New here?"), register);
        links.setSpacing(false);
        links.setPadding(false);
        links.setAlignItems(FlexComponent.Alignment.CENTER);

        add(new H1("GearAddict"), login, links);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
