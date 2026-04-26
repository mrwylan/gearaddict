package app.gearaddict.equipment;

import java.time.LocalDateTime;

public record Manufacturer(
        Long id,
        String name,
        LocalDateTime createdAt) {
}
