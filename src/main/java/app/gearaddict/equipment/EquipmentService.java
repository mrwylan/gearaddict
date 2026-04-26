package app.gearaddict.equipment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    public static final int MIN_SEARCH_TERM_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    private final EquipmentRepository equipmentRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final CatalogAuditRepository auditRepository;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            ManufacturerRepository manufacturerRepository,
                            CatalogAuditRepository auditRepository) {
        this.equipmentRepository = equipmentRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional(readOnly = true)
    public List<Equipment> browse(Optional<EquipmentCategory> category,
                                  Optional<String> searchTerm,
                                  int offset,
                                  int limit) {
        return equipmentRepository.fetchPage(category, normalize(searchTerm), offset, limit);
    }

    @Transactional(readOnly = true)
    public int count(Optional<EquipmentCategory> category, Optional<String> searchTerm) {
        return equipmentRepository.count(category, normalize(searchTerm));
    }

    @Transactional(readOnly = true)
    public Optional<Equipment> findById(Long id) {
        return equipmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public int countOwners(Long equipmentId) {
        return equipmentRepository.countOwners(equipmentId);
    }

    @Transactional
    public Equipment create(Long curatorId, EquipmentFormData form) {
        String name = normalizeName(form.name());
        validateForCreateOrUpdate(name, form);

        manufacturerRepository.findById(form.manufacturerId())
                .orElseThrow(() -> new CatalogException(
                        CatalogException.Reason.MANUFACTURER_REQUIRED,
                        "The selected manufacturer no longer exists."));

        equipmentRepository.findByNameAndManufacturer(name, form.manufacturerId()).ifPresent(existing -> {
            throw new CatalogException(
                    CatalogException.Reason.DUPLICATE_EQUIPMENT,
                    "An entry named \"" + existing.name() + "\" already exists for this manufacturer.",
                    existing.id());
        });

        String description = normalizeDescription(form.description());
        Long id = equipmentRepository.insert(name, form.manufacturerId(), form.category(), description);
        auditRepository.record(curatorId, CatalogAuditRepository.Action.CREATE,
                CatalogAuditRepository.EntityType.EQUIPMENT, id, name);
        return equipmentRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Equipment update(Long curatorId, Long equipmentId, EquipmentFormData form) {
        String name = normalizeName(form.name());
        validateForCreateOrUpdate(name, form);

        Equipment existing = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CatalogException(
                        CatalogException.Reason.NOT_FOUND, "Equipment entry not found."));

        manufacturerRepository.findById(form.manufacturerId())
                .orElseThrow(() -> new CatalogException(
                        CatalogException.Reason.MANUFACTURER_REQUIRED,
                        "The selected manufacturer no longer exists."));

        if (equipmentRepository.existsByNameAndManufacturerExcludingId(name, form.manufacturerId(), equipmentId)) {
            throw new CatalogException(
                    CatalogException.Reason.DUPLICATE_EQUIPMENT,
                    "An entry named \"" + name + "\" already exists for this manufacturer.");
        }

        String description = normalizeDescription(form.description());
        equipmentRepository.update(equipmentId, name, form.manufacturerId(), form.category(), description);
        auditRepository.record(curatorId, CatalogAuditRepository.Action.UPDATE,
                CatalogAuditRepository.EntityType.EQUIPMENT, equipmentId,
                existing.name() + " → " + name);
        return equipmentRepository.findById(equipmentId).orElseThrow();
    }

    @Transactional
    public void delete(Long curatorId, Long equipmentId) {
        Equipment existing = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CatalogException(
                        CatalogException.Reason.NOT_FOUND, "Equipment entry not found."));

        equipmentRepository.unlinkGearItemsFromEquipment(equipmentId);
        equipmentRepository.deleteById(equipmentId);
        auditRepository.record(curatorId, CatalogAuditRepository.Action.DELETE,
                CatalogAuditRepository.EntityType.EQUIPMENT, equipmentId, existing.name());
    }

    private void validateForCreateOrUpdate(String name, EquipmentFormData form) {
        if (name.isEmpty()) {
            throw new CatalogException(CatalogException.Reason.NAME_REQUIRED,
                    "Equipment name is required.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new CatalogException(CatalogException.Reason.NAME_TOO_LONG,
                    "Equipment name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (form.manufacturerId() == null) {
            throw new CatalogException(CatalogException.Reason.MANUFACTURER_REQUIRED,
                    "Manufacturer is required.");
        }
        if (form.category() == null) {
            throw new CatalogException(CatalogException.Reason.CATEGORY_REQUIRED,
                    "Category is required.");
        }
        if (form.description() != null && form.description().length() > MAX_DESCRIPTION_LENGTH) {
            throw new CatalogException(CatalogException.Reason.DESCRIPTION_TOO_LONG,
                    "Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
    }

    private static Optional<String> normalize(Optional<String> searchTerm) {
        return searchTerm
                .map(String::trim)
                .filter(term -> term.length() >= MIN_SEARCH_TERM_LENGTH);
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
