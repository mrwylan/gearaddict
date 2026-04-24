package app.gearaddict.discussion;

import java.time.LocalDateTime;

public record DiscussionThreadDetail(
        Long id,
        String title,
        String body,
        String authorUsername,
        LocalDateTime createdAt,
        Long equipmentId,
        String equipmentName,
        String equipmentManufacturer) {
}
