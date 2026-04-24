package app.gearaddict.discussion;

import java.time.LocalDateTime;

public record DiscussionThreadSummary(
        Long id,
        String title,
        String authorUsername,
        LocalDateTime createdAt,
        LocalDateTime lastReplyAt) {
}
