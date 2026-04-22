package app.gearaddict.views.inventory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "", autoLayout = false)
@PageTitle("My gear — GearAddict")
@PermitAll
public class InventoryView extends VerticalLayout {

    public static final String ROUTE = "";

    public InventoryView() {
        addClassName("inventory-view");
        setSizeFull();
        setPadding(true);

        add(new H1("Your gear inventory"),
                new Paragraph("Signed in as " + currentPrincipal()
                        + ". Your inventory is empty — add your first item soon."),
                new Button("Log out", e -> logout()));
    }

    private static String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "guest" : auth.getName();
    }

    private void logout() {
        SecurityContextHolder.clearContext();
        UI.getCurrent().getPage().setLocation("/logout");
    }
}
