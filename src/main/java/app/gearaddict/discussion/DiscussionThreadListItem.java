package app.gearaddict.discussion;

import java.time.LocalDateTime;

public record DiscussionThreadListItem(
        Long id,
        String title,
        String authorUsername,
        LocalDateTime createdAt,
        LocalDateTime lastReplyAt,
        int replyCount,
        Long equipmentId,
        String equipmentName,
        String equipmentManufacturer) {
}
