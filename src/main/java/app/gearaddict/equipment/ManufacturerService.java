package app.gearaddict.equipment;

import app.gearaddict.jooq.tables.records.ManufacturerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ManufacturerService {

    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 100;

    private final ManufacturerRepository manufacturerRepository;
    private final CatalogAuditRepository auditRepository;

    public ManufacturerService(ManufacturerRepository manufacturerRepository,
                               CatalogAuditRepository auditRepository) {
        this.manufacturerRepository = manufacturerRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional(readOnly = true)
    public List<Manufacturer> findAll() {
        return manufacturerRepository.findAllOrdered().stream()
                .map(ManufacturerService::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Manufacturer> findById(Long id) {
        return manufacturerRepository.findById(id).map(ManufacturerService::toDomain);
    }

    @Transactional(readOnly = true)
    public int countEquipmentReferencing(Long manufacturerId) {
        return manufacturerRepository.countEquipmentReferencing(manufacturerId);
    }

    @Transactional
    public Manufacturer create(Long curatorId, String rawName) {
        String name = normalize(rawName);
        validateName(name);

        manufacturerRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new CatalogException(
                    CatalogException.Reason.DUPLICATE_MANUFACTURER,
                    "A manufacturer named \"" + existing.getName() + "\" already exists.",
                    existing.getId());
        });

        ManufacturerRecord stored = manufacturerRepository.insert(name);
        auditRepository.record(curatorId, CatalogAuditRepository.Action.CREATE,
                CatalogAuditRepository.EntityType.MANUFACTURER, stored.getId(), name);
        return toDomain(stored);
    }

    @Transactional
    public Manufacturer rename(Long curatorId, Long manufacturerId, String rawName) {
        String name = normalize(rawName);
        validateName(name);

        ManufacturerRecord existing = manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> new CatalogException(
                        CatalogException.Reason.NOT_FOUND,
                        "Manufacturer not found."));

        if (manufacturerRepository.existsByNameIgnoreCaseExcludingId(name, manufacturerId)) {
            throw new CatalogException(
                    CatalogException.Reason.DUPLICATE_MANUFACTURER,
                    "A manufacturer named \"" + name + "\" already exists.");
        }

        ManufacturerRecord updated = manufacturerRepository.updateName(manufacturerId, name);
        auditRepository.record(curatorId, CatalogAuditRepository.Action.UPDATE,
                CatalogAuditRepository.EntityType.MANUFACTURER, manufacturerId,
                existing.getName() + " → " + name);
        return toDomain(updated);
    }

    @Transactional
    public void delete(Long curatorId, Long manufacturerId) {
        ManufacturerRecord existing = manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> new CatalogException(
                        CatalogException.Reason.NOT_FOUND,
                        "Manufacturer not found."));

        int referencing = manufacturerRepository.countEquipmentReferencing(manufacturerId);
        if (referencing > 0) {
            throw new CatalogException(
                    CatalogException.Reason.MANUFACTURER_IN_USE,
                    "Cannot remove this manufacturer while " + referencing
                            + " equipment " + (referencing == 1 ? "entry" : "entries")
                            + " still reference it. Reassign or remove those entries first.");
        }

        manufacturerRepository.deleteById(manufacturerId);
        auditRepository.record(curatorId, CatalogAuditRepository.Action.DELETE,
                CatalogAuditRepository.EntityType.MANUFACTURER, manufacturerId, existing.getName());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static void validateName(String name) {
        if (name.isEmpty()) {
            throw new CatalogException(CatalogException.Reason.NAME_REQUIRED,
                    "Manufacturer name is required.");
        }
        if (name.length() < MIN_NAME_LENGTH) {
            throw new CatalogException(CatalogException.Reason.NAME_TOO_SHORT,
                    "Manufacturer name must be at least " + MIN_NAME_LENGTH + " characters.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new CatalogException(CatalogException.Reason.NAME_TOO_LONG,
                    "Manufacturer name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
    }

    private static Manufacturer toDomain(ManufacturerRecord record) {
        return new Manufacturer(record.getId(), record.getName(), record.getCreatedAt());
    }
}
