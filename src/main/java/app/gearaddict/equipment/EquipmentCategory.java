package app.gearaddict.equipment;

import java.util.Optional;

public enum EquipmentCategory {
    SYNTH("Synth"),
    EFFECT("Effect"),
    KEYBOARD("Keyboard"),
    INTERFACE("Interface"),
    OTHER("Other");

    private final String label;

    EquipmentCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<EquipmentCategory> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        for (EquipmentCategory category : values()) {
            if (category.label.equals(label)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
