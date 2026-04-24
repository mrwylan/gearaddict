package app.gearaddict.gear;

import app.gearaddict.equipment.EquipmentCategory;

import java.time.LocalDateTime;

public record GearItem(
        Long id,
        Long userId,
        Long equipmentId,
        String equipmentName,
        String equipmentManufacturer,
        String name,
        EquipmentCategory category,
        String notes,
        LocalDateTime createdAt) {

    public String displayName() {
        return equipmentName != null ? equipmentName : name;
    }

    public String displayManufacturer() {
        return equipmentManufacturer;
    }
}
