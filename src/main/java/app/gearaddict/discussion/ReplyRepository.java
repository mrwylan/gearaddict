package app.gearaddict.discussion;

import org.jooq.DSLContext;
import org.jooq.Record4;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static app.gearaddict.jooq.Tables.REPLY;
import static app.gearaddict.jooq.Tables.USERS;

@Repository
public class ReplyRepository {

    private final DSLContext dsl;

    public ReplyRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<DiscussionReply> fetchPageByThread(Long threadId, int offset, int limit) {
        return dsl.select(
                        REPLY.ID,
                        USERS.USERNAME,
                        REPLY.BODY,
                        REPLY.CREATED_AT)
                .from(REPLY)
                .join(USERS).on(USERS.ID.eq(REPLY.AUTHOR_ID))
                .where(REPLY.DISCUSSION_THREAD_ID.eq(threadId))
                .orderBy(REPLY.CREATED_AT.asc(), REPLY.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(ReplyRepository::toDomain);
    }

    public int countByThread(Long threadId) {
        return dsl.fetchCount(
                dsl.selectFrom(REPLY)
                        .where(REPLY.DISCUSSION_THREAD_ID.eq(threadId)));
    }

    public Long insert(Long threadId, Long authorId, String body) {
        return dsl.insertInto(REPLY)
                .set(REPLY.DISCUSSION_THREAD_ID, threadId)
                .set(REPLY.AUTHOR_ID, authorId)
                .set(REPLY.BODY, body)
                .returning(REPLY.ID)
                .fetchOne()
                .getId();
    }

    private static DiscussionReply toDomain(Record4<Long, String, String, LocalDateTime> r) {
        return new DiscussionReply(r.value1(), r.value2(), r.value3(), r.value4());
    }
}
