package app.gearaddict.discussion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DiscussionThreadService {

    public static final int EQUIPMENT_PAGE_SUMMARY_LIMIT = 5;

    private final DiscussionThreadRepository repository;
    private final ReplyRepository replyRepository;

    public DiscussionThreadService(DiscussionThreadRepository repository,
                                   ReplyRepository replyRepository) {
        this.repository = repository;
        this.replyRepository = replyRepository;
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
}
