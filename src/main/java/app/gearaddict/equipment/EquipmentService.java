package app.gearaddict.equipment;

import app.gearaddict.jooq.tables.records.EquipmentRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Equipment> browse(Optional<EquipmentCategory> category, int offset, int limit) {
        return equipmentRepository.fetchPage(category, offset, limit).stream()
                .map(EquipmentService::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public int count(Optional<EquipmentCategory> category) {
        return equipmentRepository.count(category);
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
