package app.gearaddict.connection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GearConnectionService {

    private final GearConnectionRepository repository;

    public GearConnectionService(GearConnectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean hasCatalogLinkedItems(Long userId) {
        return repository.hasCatalogLinkedItems(userId);
    }

    @Transactional(readOnly = true)
    public int countConnections(Long userId) {
        return repository.countConnections(userId);
    }

    @Transactional(readOnly = true)
    public List<GearConnection> findConnections(Long userId, int offset, int limit) {
        return repository.findConnections(userId, offset, limit);
    }
}
