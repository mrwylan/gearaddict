package app.gearaddict.views.auth;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class LoginViewTest {

    private final Routes routes = new Routes();

    @BeforeEach
    void setUp() {
        MockVaadin.setup(routes);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void loginFormPostsToSpringSecurityLoginEndpoint() {
        LoginView view = new LoginView();
        UI.getCurrent().add(view);

        LoginForm form = _get(LoginForm.class);
        assertThat(form.getAction()).isEqualTo("login");
        assertThat(form.isForgotPasswordButtonVisible()).isFalse();
    }

    @Test
    void pageDisplaysAppTitle() {
        LoginView view = new LoginView();
        UI.getCurrent().add(view);

        assertThat(_get(H1.class).getText()).isEqualTo("GearAddict");
    }
}
