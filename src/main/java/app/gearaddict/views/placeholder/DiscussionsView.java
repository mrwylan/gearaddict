package app.gearaddict.views.placeholder;

import app.gearaddict.views.ComingSoonView;
import app.gearaddict.views.MainLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "discussions", layout = MainLayout.class)
@PageTitle("Discussions — GearAddict")
@AnonymousAllowed
public class DiscussionsView extends ComingSoonView {

    public DiscussionsView() {
        super("Discussions", "UC-008");
    }
}
