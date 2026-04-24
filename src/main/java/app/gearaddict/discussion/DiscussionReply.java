package app.gearaddict.discussion;

import java.time.LocalDateTime;

public record DiscussionReply(
        Long id,
        String authorUsername,
        String body,
        LocalDateTime createdAt) {
}
