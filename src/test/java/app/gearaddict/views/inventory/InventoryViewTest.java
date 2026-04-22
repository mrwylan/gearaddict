package app.gearaddict.views.inventory;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryViewTest {

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
    void logoutButtonInvokesAuthenticationContextLogout() {
        AuthenticationContext auth = mock(AuthenticationContext.class);
        when(auth.getPrincipalName()).thenReturn(Optional.of("alice@example.com"));

        InventoryView view = new InventoryView(auth);
        UI.getCurrent().add(view);

        _click(_get(Button.class, spec -> spec.withId("logout")));

        verify(auth).logout();
    }
}
