package app.gearaddict.discussion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiscussionThreadService {

    public static final int EQUIPMENT_PAGE_SUMMARY_LIMIT = 5;

    private final DiscussionThreadRepository repository;

    public DiscussionThreadService(DiscussionThreadRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DiscussionThreadSummary> recentForEquipment(Long equipmentId, int limit) {
        return repository.fetchRecentByEquipment(equipmentId, limit);
    }

    @Transactional(readOnly = true)
    public int countForEquipment(Long equipmentId) {
        return repository.countByEquipment(equipmentId);
    }
}
