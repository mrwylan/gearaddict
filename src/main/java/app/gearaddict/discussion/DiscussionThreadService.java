package app.gearaddict.discussion;

import app.gearaddict.equipment.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DiscussionThreadService {

    public static final int EQUIPMENT_PAGE_SUMMARY_LIMIT = 5;

    public static final int MIN_TITLE_LENGTH = 5;
    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MIN_BODY_LENGTH = 10;
    public static final int MAX_BODY_LENGTH = 10_000;

    public static final int MIN_REPLY_LENGTH = 1;
    public static final int MAX_REPLY_LENGTH = 10_000;

    private final DiscussionThreadRepository repository;
    private final ReplyRepository replyRepository;
    private final EquipmentRepository equipmentRepository;

    public DiscussionThreadService(DiscussionThreadRepository repository,
                                   ReplyRepository replyRepository,
                                   EquipmentRepository equipmentRepository) {
        this.repository = repository;
        this.replyRepository = replyRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DiscussionThreadSummary> recentForEquipment(Long equipmentId, int limit) {
        return repository.fetchRecentByEquipment(equipmentId, limit);
    }

    @Transactional(readOnly = true)
    public List<DiscussionThreadSummary> listForEquipment(Long equipmentId, int offset, int limit) {
        return repository.fetchSummariesByEquipment(equipmentId, offset, limit);
    }

    @Transactional(readOnly = true)
    public int countForEquipment(Long equipmentId) {
        return repository.countByEquipment(equipmentId);
    }

    @Transactional(readOnly = true)
    public List<DiscussionThreadListItem> listAggregated(int offset, int limit) {
        return repository.fetchAggregatedPage(offset, limit);
    }

    @Transactional(readOnly = true)
    public int countAll() {
        return repository.countAll();
    }

    @Transactional(readOnly = true)
    public Optional<DiscussionThreadDetail> findDetail(Long threadId) {
        return repository.findDetailById(threadId);
    }

    @Transactional(readOnly = true)
    public List<DiscussionReply> listReplies(Long threadId, int offset, int limit) {
        return replyRepository.fetchPageByThread(threadId, offset, limit);
    }

    @Transactional(readOnly = true)
    public int countReplies(Long threadId) {
        return replyRepository.countByThread(threadId);
    }

    /**
     * Creates a new discussion thread on the given equipment, attributed to the author.
     * Returns the new thread's ID. Validates title and body length per UC-015 BR-002/BR-003.
     */
    @Transactional
    public Long startThread(Long equipmentId, Long authorId, String title, String body) {
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedBody = body == null ? "" : body.trim();

        if (trimmedTitle.isEmpty()) {
            throw new DiscussionThreadException(
                    DiscussionThreadException.Reason.TITLE_REQUIRED,
                    "Title is required.");
        }
        if (trimmedTitle.length() < MIN_TITLE_LENGTH) {
            throw new DiscussionThreadException(
                    DiscussionThreadException.Reason.TITLE_TOO_SHORT,
                    "Title must be at least " + MIN_TITLE_LENGTH + " characters.");
        }
        if (trimmedTitle.length() > MAX_TITLE_LENGTH) {
            throw new DiscussionThreadException(
                    DiscussionThreadException.Reason.TITLE_TOO_LONG,
                    "Title must be at most " + MAX_TITLE_LENGTH + " characters.");
        }
        if (trimmedBody.isEmpty()) {
            throw new DiscussionThreadException(
                    DiscussionThreadException.Reason.BODY_REQUIRED,
                    "Opening post is required.");
        }
        if (trimmedBody.length() < MIN_BODY_LENGTH) {
            throw new DiscussionThreadException(
                    DiscussionThreadException.Reason.BODY_TOO_SHORT,
                    "Opening post must be at least " + MIN_BODY_LENGTH + " characters.");
        }
        if (trimmedBody.length() > MAX_BODY_LENGTH) {
            throw new DiscussionThreadException(
                    DiscussionThreadException.Reason.BODY_TOO_LONG,
                    "Opening post must be at most " + MAX_BODY_LENGTH + " characters.");
        }
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            throw new DiscussionThreadException(
                    DiscussionThreadException.Reason.EQUIPMENT_NOT_FOUND,
                    "Equipment does not exist.");
        }
        return repository.insert(equipmentId, authorId, trimmedTitle, trimmedBody);
    }

    /**
     * Appends a reply to the given thread, attributed to the author. Bumps
     * {@code last_reply_at} so the thread floats to the top of recent-activity lists.
     * Validates body length per UC-016 BR-002.
     */
    @Transactional
    public Long reply(Long threadId, Long authorId, String body) {
        String trimmed = body == null ? "" : body.trim();

        if (trimmed.isEmpty()) {
            throw new ReplyException(
                    ReplyException.Reason.BODY_REQUIRED,
                    "Reply is required.");
        }
        if (trimmed.length() > MAX_REPLY_LENGTH) {
            throw new ReplyException(
                    ReplyException.Reason.BODY_TOO_LONG,
                    "Reply must be at most " + MAX_REPLY_LENGTH + " characters.");
        }
        if (!repository.exists(threadId)) {
            throw new ReplyException(
                    ReplyException.Reason.THREAD_NOT_FOUND,
                    "Discussion thread does not exist.");
        }

        Long replyId = replyRepository.insert(threadId, authorId, trimmed);
        repository.touchLastReplyAt(threadId, LocalDateTime.now());
        return replyId;
    }
}
