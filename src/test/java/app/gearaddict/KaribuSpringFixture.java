package app.gearaddict;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import kotlin.jvm.functions.Function0;
import org.springframework.context.ApplicationContext;

public final class KaribuSpringFixture {

    private KaribuSpringFixture() {}

    public static void setUp(ApplicationContext ctx, Routes routes) {
        Function0<UI> uiFactory = UI::new;
        MockVaadin.setup(uiFactory, new MockSpringServlet(routes, ctx, uiFactory));
    }

    public static void tearDown() {
        MockVaadin.tearDown();
    }
}
