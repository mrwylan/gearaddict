package app.gearaddict.equipment;

public record EquipmentFormData(
        String name,
        Long manufacturerId,
        EquipmentCategory category,
        String description) {
}
