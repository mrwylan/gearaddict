package app.gearaddict.views.placeholder;

import app.gearaddict.views.ComingSoonView;
import app.gearaddict.views.MainLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "catalog", layout = MainLayout.class)
@PageTitle("Equipment Catalog — GearAddict")
@AnonymousAllowed
public class CatalogView extends ComingSoonView {

    public CatalogView() {
        super("Equipment Catalog", "UC-005");
    }
}
