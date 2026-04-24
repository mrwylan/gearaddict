package app.gearaddict.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class ComingSoonView extends VerticalLayout {

    protected ComingSoonView(String featureName, String useCaseId) {
        addClassName("coming-soon-view");
        setSizeFull();
        setPadding(true);
        add(new H1(featureName),
                new Paragraph("Coming soon — implementation tracked under " + useCaseId + "."));
    }
}
