package app.gearaddict.discussion;

import org.jooq.DSLContext;
import org.jooq.Record5;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static app.gearaddict.jooq.Tables.DISCUSSION_THREAD;
import static app.gearaddict.jooq.Tables.USERS;
import static org.jooq.impl.DSL.coalesce;

@Repository
public class DiscussionThreadRepository {

    private final DSLContext dsl;

    public DiscussionThreadRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<DiscussionThreadSummary> fetchRecentByEquipment(Long equipmentId, int limit) {
        return dsl.select(
                        DISCUSSION_THREAD.ID,
                        DISCUSSION_THREAD.TITLE,
                        USERS.USERNAME,
                        DISCUSSION_THREAD.CREATED_AT,
                        DISCUSSION_THREAD.LAST_REPLY_AT)
                .from(DISCUSSION_THREAD)
                .join(USERS).on(USERS.ID.eq(DISCUSSION_THREAD.AUTHOR_ID))
                .where(DISCUSSION_THREAD.EQUIPMENT_ID.eq(equipmentId))
                .orderBy(coalesce(DISCUSSION_THREAD.LAST_REPLY_AT, DISCUSSION_THREAD.CREATED_AT).desc())
                .limit(limit)
                .fetch(DiscussionThreadRepository::toSummary);
    }

    public int countByEquipment(Long equipmentId) {
        return dsl.fetchCount(
                dsl.selectFrom(DISCUSSION_THREAD)
                        .where(DISCUSSION_THREAD.EQUIPMENT_ID.eq(equipmentId)));
    }

    private static DiscussionThreadSummary toSummary(Record5<Long, String, String, LocalDateTime, LocalDateTime> r) {
        return new DiscussionThreadSummary(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5());
    }
}
