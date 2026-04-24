package app.gearaddict.views.placeholder;

import app.gearaddict.views.ComingSoonView;
import app.gearaddict.views.MainLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "connections", layout = MainLayout.class)
@PageTitle("Gear Connections — GearAddict")
@PermitAll
public class ConnectionsView extends ComingSoonView {

    public ConnectionsView() {
        super("Gear Connections", "UC-014");
    }
}
