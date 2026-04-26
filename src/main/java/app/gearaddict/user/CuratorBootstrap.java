package app.gearaddict.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CuratorBootstrap {

    private static final Logger log = LoggerFactory.getLogger(CuratorBootstrap.class);

    private final UserRepository userRepository;
    private final List<String> curatorEmails;

    public CuratorBootstrap(UserRepository userRepository,
                            @Value("${app.curator-emails:}") List<String> curatorEmails) {
        this.userRepository = userRepository;
        this.curatorEmails = curatorEmails;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void grantConfiguredCurators() {
        for (String email : curatorEmails) {
            String trimmed = email.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int updated = userRepository.setCuratorByEmail(trimmed, true);
            if (updated > 0) {
                log.info("Granted catalog curation permission to {}", trimmed);
            }
        }
    }
}
