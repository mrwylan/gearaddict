package app.gearaddict.gear;

import app.gearaddict.equipment.EquipmentCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GearItemService {

    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_NOTES_LENGTH = 1000;

    private final GearItemRepository repository;

    public GearItemService(GearItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GearItem> listForUser(Long userId, Optional<EquipmentCategory> category, int offset, int limit) {
        return repository.fetchForUser(userId, category, offset, limit);
    }

    @Transactional(readOnly = true)
    public int countForUser(Long userId, Optional<EquipmentCategory> category) {
        return repository.countForUser(userId, category);
    }

    @Transactional(readOnly = true)
    public Optional<GearItem> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicateForUser(Long userId, Long equipmentId, String freeTextName) {
        if (equipmentId != null) {
            return repository.existsForUserWithEquipment(userId, equipmentId);
        }
        if (freeTextName != null && !freeTextName.isBlank()) {
            return repository.existsForUserWithName(userId, freeTextName.trim());
        }
        return false;
    }

    @Transactional
    public GearItem add(Long userId, GearItemFormData form) {
        validate(form);
        String freeTextName = normalize(form.freeTextName());
        String notes = normalize(form.notes());
        Long id = repository.insert(userId, form.equipmentId(), freeTextName, form.category(), notes);
        return repository.findById(id).orElseThrow(
                () -> new GearItemException(GearItemException.Reason.NOT_FOUND,
                        "Gear item disappeared immediately after insert."));
    }

    @Transactional
    public void remove(Long userId, Long gearItemId) {
        GearItem existing = repository.findById(gearItemId).orElseThrow(
                () -> new GearItemException(GearItemException.Reason.NOT_FOUND,
                        "Gear item does not exist."));
        if (!existing.userId().equals(userId)) {
            throw new GearItemException(GearItemException.Reason.NOT_OWNER,
                    "You may only remove items from your own inventory.");
        }
        repository.deleteById(gearItemId);
    }

    @Transactional
    public GearItem update(Long userId, Long gearItemId, GearItemFormData form) {
        GearItem existing = repository.findById(gearItemId).orElseThrow(
                () -> new GearItemException(GearItemException.Reason.NOT_FOUND,
                        "Gear item does not exist."));
        if (!existing.userId().equals(userId)) {
            throw new GearItemException(GearItemException.Reason.NOT_OWNER,
                    "You may only edit items in your own inventory.");
        }
        validate(form);
        String freeTextName = normalize(form.freeTextName());
        String notes = normalize(form.notes());
        repository.update(gearItemId, form.equipmentId(), freeTextName, form.category(), notes);
        return repository.findById(gearItemId).orElseThrow();
    }

    private static void validate(GearItemFormData form) {
        if (form.category() == null) {
            throw new GearItemException(GearItemException.Reason.CATEGORY_REQUIRED,
                    "Category is required.");
        }
        boolean hasEquipment = form.equipmentId() != null;
        boolean hasFreeText = form.freeTextName() != null && !form.freeTextName().isBlank();
        if (!hasEquipment && !hasFreeText) {
            throw new GearItemException(GearItemException.Reason.EQUIPMENT_OR_NAME_REQUIRED,
                    "Select a catalog entry or enter a device name.");
        }
        if (hasFreeText && form.freeTextName().trim().length() > MAX_NAME_LENGTH) {
            throw new GearItemException(GearItemException.Reason.NAME_TOO_LONG,
                    "Device name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (form.notes() != null && form.notes().length() > MAX_NOTES_LENGTH) {
            throw new GearItemException(GearItemException.Reason.NOTES_TOO_LONG,
                    "Notes must be at most " + MAX_NOTES_LENGTH + " characters.");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
