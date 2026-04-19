package app.gearaddict;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Theme("gearaddict")
public class GearAddictApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(GearAddictApplication.class, args);
    }
}
