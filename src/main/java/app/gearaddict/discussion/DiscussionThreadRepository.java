package app.gearaddict.discussion;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record6;
import org.jooq.Record8;
import org.jooq.Record9;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static app.gearaddict.jooq.Tables.DISCUSSION_THREAD;
import static app.gearaddict.jooq.Tables.EQUIPMENT;
import static app.gearaddict.jooq.Tables.REPLY;
import static app.gearaddict.jooq.Tables.USERS;
import static org.jooq.impl.DSL.coalesce;

@Repository
public class DiscussionThreadRepository {

    private final DSLContext dsl;

    public DiscussionThreadRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<DiscussionThreadSummary> fetchRecentByEquipment(Long equipmentId, int limit) {
        return fetchSummariesByEquipment(equipmentId, 0, limit);
    }

    public List<DiscussionThreadSummary> fetchSummariesByEquipment(Long equipmentId, int offset, int limit) {
        Field<Integer> replyCount = replyCountField();
        return dsl.select(
                        DISCUSSION_THREAD.ID,
                        DISCUSSION_THREAD.TITLE,
                        USERS.USERNAME,
                        DISCUSSION_THREAD.CREATED_AT,
                        DISCUSSION_THREAD.LAST_REPLY_AT,
                        replyCount)
                .from(DISCUSSION_THREAD)
                .join(USERS).on(USERS.ID.eq(DISCUSSION_THREAD.AUTHOR_ID))
                .where(DISCUSSION_THREAD.EQUIPMENT_ID.eq(equipmentId))
                .orderBy(coalesce(DISCUSSION_THREAD.LAST_REPLY_AT, DISCUSSION_THREAD.CREATED_AT).desc())
                .offset(offset)
                .limit(limit)
                .fetch(DiscussionThreadRepository::toSummary);
    }

    public int countByEquipment(Long equipmentId) {
        return dsl.fetchCount(
                dsl.selectFrom(DISCUSSION_THREAD)
                        .where(DISCUSSION_THREAD.EQUIPMENT_ID.eq(equipmentId)));
    }

    public List<DiscussionThreadListItem> fetchAggregatedPage(int offset, int limit) {
        Field<Integer> replyCount = replyCountField();
        return dsl.select(
                        DISCUSSION_THREAD.ID,
                        DISCUSSION_THREAD.TITLE,
                        USERS.USERNAME,
                        DISCUSSION_THREAD.CREATED_AT,
                        DISCUSSION_THREAD.LAST_REPLY_AT,
                        replyCount,
                        EQUIPMENT.ID,
                        EQUIPMENT.NAME,
                        EQUIPMENT.MANUFACTURER)
                .from(DISCUSSION_THREAD)
                .join(USERS).on(USERS.ID.eq(DISCUSSION_THREAD.AUTHOR_ID))
                .join(EQUIPMENT).on(EQUIPMENT.ID.eq(DISCUSSION_THREAD.EQUIPMENT_ID))
                .orderBy(coalesce(DISCUSSION_THREAD.LAST_REPLY_AT, DISCUSSION_THREAD.CREATED_AT).desc())
                .offset(offset)
                .limit(limit)
                .fetch(DiscussionThreadRepository::toListItem);
    }

    public int countAll() {
        return dsl.fetchCount(dsl.selectFrom(DISCUSSION_THREAD));
    }

    public Long insert(Long equipmentId, Long authorId, String title, String body) {
        return dsl.insertInto(DISCUSSION_THREAD)
                .set(DISCUSSION_THREAD.EQUIPMENT_ID, equipmentId)
                .set(DISCUSSION_THREAD.AUTHOR_ID, authorId)
                .set(DISCUSSION_THREAD.TITLE, title)
                .set(DISCUSSION_THREAD.BODY, body)
                .returning(DISCUSSION_THREAD.ID)
                .fetchOne()
                .getId();
    }

    public boolean exists(Long threadId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(DISCUSSION_THREAD)
                        .where(DISCUSSION_THREAD.ID.eq(threadId)));
    }

    public void touchLastReplyAt(Long threadId, LocalDateTime timestamp) {
        dsl.update(DISCUSSION_THREAD)
                .set(DISCUSSION_THREAD.LAST_REPLY_AT, timestamp)
                .where(DISCUSSION_THREAD.ID.eq(threadId))
                .execute();
    }

    public Optional<DiscussionThreadDetail> findDetailById(Long threadId) {
        return dsl.select(
                        DISCUSSION_THREAD.ID,
                        DISCUSSION_THREAD.TITLE,
                        DISCUSSION_THREAD.BODY,
                        USERS.USERNAME,
                        DISCUSSION_THREAD.CREATED_AT,
                        EQUIPMENT.ID,
                        EQUIPMENT.NAME,
                        EQUIPMENT.MANUFACTURER)
                .from(DISCUSSION_THREAD)
                .join(USERS).on(USERS.ID.eq(DISCUSSION_THREAD.AUTHOR_ID))
                .join(EQUIPMENT).on(EQUIPMENT.ID.eq(DISCUSSION_THREAD.EQUIPMENT_ID))
                .where(DISCUSSION_THREAD.ID.eq(threadId))
                .fetchOptional(DiscussionThreadRepository::toDetail);
    }

    private Field<Integer> replyCountField() {
        return DSL.field(
                DSL.selectCount()
                        .from(REPLY)
                        .where(REPLY.DISCUSSION_THREAD_ID.eq(DISCUSSION_THREAD.ID)));
    }

    private static DiscussionThreadSummary toSummary(Record6<Long, String, String, LocalDateTime, LocalDateTime, Integer> r) {
        return new DiscussionThreadSummary(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6() == null ? 0 : r.value6());
    }

    private static DiscussionThreadListItem toListItem(Record9<Long, String, String, LocalDateTime, LocalDateTime, Integer, Long, String, String> r) {
        return new DiscussionThreadListItem(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6() == null ? 0 : r.value6(),
                r.value7(),
                r.value8(),
                r.value9());
    }

    private static DiscussionThreadDetail toDetail(Record8<Long, String, String, String, LocalDateTime, Long, String, String> r) {
        return new DiscussionThreadDetail(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6(),
                r.value7(),
                r.value8());
    }
}
