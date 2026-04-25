package app.gearaddict.connection;

import java.util.List;

public record GearConnection(Long userId, String username, List<SharedEquipment> sharedEquipment) {
}
