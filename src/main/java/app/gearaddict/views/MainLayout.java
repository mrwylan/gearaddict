package app.gearaddict.views;

import app.gearaddict.views.inventory.InventoryView;
import app.gearaddict.views.placeholder.CatalogView;
import app.gearaddict.views.placeholder.ConnectionsView;
import app.gearaddict.views.placeholder.DiscussionsView;
import app.gearaddict.views.profile.ProfileView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Application shell — left-hand navigation rail per FR-017 / FR-018 and
 * docs/requirements.md § Navigation Layout. Wraps every top-level view listed
 * in the Use Case Mapping table; auth-gate views (login, register) are exempt.
 */
public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authenticationContext;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Toggle navigation");
        toggle.setId("nav-toggle");
        addToNavbar(toggle);

        addToDrawer(buildDrawerContent());
    }

    private VerticalLayout buildDrawerContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);

        content.add(buildSideNav());

        Div spacer = new Div();
        content.add(spacer);
        content.expand(spacer);

        content.add(buildAvatarFooter());
        return content;
    }

    private SideNav buildSideNav() {
        SideNav nav = new SideNav();
        boolean authenticated = authenticationContext.isAuthenticated();

        if (authenticated) {
            nav.addItem(new SideNavItem("My Inventory", InventoryView.class,
                    VaadinIcon.RECORDS.create()));
        }
        nav.addItem(new SideNavItem("Equipment Catalog", CatalogView.class,
                VaadinIcon.GRID_BIG.create()));
        nav.addItem(new SideNavItem("Discussions", DiscussionsView.class,
                VaadinIcon.COMMENTS.create()));
        if (authenticated) {
            nav.addItem(new SideNavItem("Gear Connections", ConnectionsView.class,
                    VaadinIcon.CONNECT.create()));
        }
        return nav;
    }

    private Footer buildAvatarFooter() {
        Footer footer = new Footer();
        if (authenticationContext.isAuthenticated()) {
            String name = authenticationContext.getPrincipalName().orElse("User");
            Avatar avatar = new Avatar(name);

            RouterLink profileLink = new RouterLink();
            profileLink.setRoute(ProfileView.class);
            profileLink.add(avatar);
            profileLink.setId("profile-link");

            footer.add(profileLink);
        }
        return footer;
    }
}
