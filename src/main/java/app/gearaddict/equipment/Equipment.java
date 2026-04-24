package app.gearaddict.equipment;

public record Equipment(
        Long id,
        String name,
        String manufacturer,
        EquipmentCategory category,
        String description) {
}
