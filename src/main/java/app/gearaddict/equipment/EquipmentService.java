package app.gearaddict.equipment;

import app.gearaddict.jooq.tables.records.EquipmentRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    public static final int MIN_SEARCH_TERM_LENGTH = 2;

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Equipment> browse(Optional<EquipmentCategory> category,
                                  Optional<String> searchTerm,
                                  int offset,
                                  int limit) {
        return equipmentRepository.fetchPage(category, normalize(searchTerm), offset, limit).stream()
                .map(EquipmentService::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public int count(Optional<EquipmentCategory> category, Optional<String> searchTerm) {
        return equipmentRepository.count(category, normalize(searchTerm));
    }

    @Transactional(readOnly = true)
    public Optional<Equipment> findById(Long id) {
        return equipmentRepository.findById(id).map(EquipmentService::toDomain);
    }

    @Transactional(readOnly = true)
    public int countOwners(Long equipmentId) {
        return equipmentRepository.countOwners(equipmentId);
    }

    private static Optional<String> normalize(Optional<String> searchTerm) {
        return searchTerm
                .map(String::trim)
                .filter(term -> term.length() >= MIN_SEARCH_TERM_LENGTH);
    }

    private static Equipment toDomain(EquipmentRecord record) {
        return new Equipment(
                record.getId(),
                record.getName(),
                record.getManufacturer(),
                EquipmentCategory.fromLabel(record.getCategory()).orElse(EquipmentCategory.OTHER),
                record.getDescription());
    }
}
