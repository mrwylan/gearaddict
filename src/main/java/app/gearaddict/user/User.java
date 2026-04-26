package app.gearaddict.user;

import java.time.LocalDateTime;

public record User(
        Long id,
        String username,
        String email,
        String bio,
        boolean publicInventory,
        boolean curator,
        LocalDateTime createdAt) {
}
