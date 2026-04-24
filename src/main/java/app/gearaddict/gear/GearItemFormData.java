package app.gearaddict.gear;

import app.gearaddict.equipment.EquipmentCategory;

public record GearItemFormData(
        Long equipmentId,
        String freeTextName,
        EquipmentCategory category,
        String notes) {
}
