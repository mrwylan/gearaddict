package app.gearaddict.views.inventory;

import app.gearaddict.views.MainLayout;
import app.gearaddict.views.profile.ProfileView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@PageTitle("My gear — GearAddict")
@PermitAll
public class InventoryView extends VerticalLayout {

    public static final String ROUTE = "";

    private final transient AuthenticationContext authenticationContext;

    public InventoryView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        addClassName("inventory-view");
        setSizeFull();
        setPadding(true);

        String principal = authenticationContext.getPrincipalName().orElse("guest");

        Button profile = new Button("Edit profile",
                e -> UI.getCurrent().navigate(ProfileView.ROUTE));
        profile.setId("edit-profile");

        Button logout = new Button("Log out", e -> authenticationContext.logout());
        logout.setId("logout");

        add(new H1("Your gear inventory"),
                new Paragraph("Signed in as " + principal
                        + ". Your inventory is empty — add your first item soon."),
                new HorizontalLayout(profile, logout));
    }
}
