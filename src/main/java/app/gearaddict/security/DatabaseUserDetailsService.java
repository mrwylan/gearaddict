package app.gearaddict.security;

import app.gearaddict.jooq.tables.records.UsersRecord;
import app.gearaddict.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        UsersRecord record = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + usernameOrEmail));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        if (Boolean.TRUE.equals(record.getCurator())) {
            authorities.add(new SimpleGrantedAuthority(ROLE_CURATOR));
        }

        return new User(record.getEmail(), record.getPassword(), authorities);
    }

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_CURATOR = "ROLE_CURATOR";
}
