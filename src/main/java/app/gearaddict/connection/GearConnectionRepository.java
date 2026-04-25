package app.gearaddict.connection;

import org.jooq.DSLContext;
import org.jooq.Record4;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static app.gearaddict.jooq.Tables.EQUIPMENT;
import static app.gearaddict.jooq.Tables.GEAR_ITEM;
import static app.gearaddict.jooq.Tables.USERS;

@Repository
public class GearConnectionRepository {

    private final DSLContext dsl;

    public GearConnectionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Returns true iff the given user has at least one gear item linked to a catalog
     * equipment entry. Free-text-only inventories cannot participate in connections
     * (UC-014 BR-001).
     */
    public boolean hasCatalogLinkedItems(Long userId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(GEAR_ITEM)
                        .where(GEAR_ITEM.USER_ID.eq(userId))
                        .and(GEAR_ITEM.EQUIPMENT_ID.isNotNull()));
    }

    /**
     * Counts distinct users that share at least one catalog-linked equipment with the
     * given user, excluding self and respecting public_inventory (BR-002, BR-003).
     */
    public int countConnections(Long userId) {
        return dsl.fetchCount(
                dsl.selectDistinct(USERS.ID)
                        .from(USERS)
                        .join(GEAR_ITEM).on(GEAR_ITEM.USER_ID.eq(USERS.ID))
                        .where(USERS.PUBLIC_INVENTORY.isTrue())
                        .and(USERS.ID.ne(userId))
                        .and(GEAR_ITEM.EQUIPMENT_ID.in(ownedEquipmentIds(userId))));
    }

    /**
     * Returns connections paginated by user. For each connected user, includes the list
     * of equipment they share with the requesting user (catalog-linked only). Order is
     * deterministic: alphabetical by username, then alphabetical by shared equipment name.
     */
    public List<GearConnection> findConnections(Long userId, int offset, int limit) {
        List<Long> connectedUserIds = dsl
                .selectDistinct(USERS.ID, USERS.USERNAME)
                .from(USERS)
                .join(GEAR_ITEM).on(GEAR_ITEM.USER_ID.eq(USERS.ID))
                .where(USERS.PUBLIC_INVENTORY.isTrue())
                .and(USERS.ID.ne(userId))
                .and(GEAR_ITEM.EQUIPMENT_ID.in(ownedEquipmentIds(userId)))
                .orderBy(USERS.USERNAME.asc(), USERS.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(USERS.ID);

        if (connectedUserIds.isEmpty()) {
            return List.of();
        }

        // Insertion order matches the username order from the page query above.
        Map<Long, GearConnection> byUser = new LinkedHashMap<>();
        dsl.selectDistinct(USERS.ID, USERS.USERNAME, EQUIPMENT.ID, EQUIPMENT.NAME)
                .from(USERS)
                .join(GEAR_ITEM).on(GEAR_ITEM.USER_ID.eq(USERS.ID))
                .join(EQUIPMENT).on(EQUIPMENT.ID.eq(GEAR_ITEM.EQUIPMENT_ID))
                .where(USERS.ID.in(connectedUserIds))
                .and(EQUIPMENT.ID.in(ownedEquipmentIds(userId)))
                .orderBy(USERS.USERNAME.asc(), USERS.ID.asc(), EQUIPMENT.NAME.asc())
                .fetch()
                .forEach(record -> accumulate(byUser, record));

        return new ArrayList<>(byUser.values());
    }

    private org.jooq.Select<org.jooq.Record1<Long>> ownedEquipmentIds(Long userId) {
        return dsl.selectDistinct(GEAR_ITEM.EQUIPMENT_ID)
                .from(GEAR_ITEM)
                .where(GEAR_ITEM.USER_ID.eq(userId))
                .and(GEAR_ITEM.EQUIPMENT_ID.isNotNull());
    }

    private static void accumulate(Map<Long, GearConnection> byUser,
                                   Record4<Long, String, Long, String> record) {
        Long uid = record.value1();
        GearConnection existing = byUser.get(uid);
        SharedEquipment shared = new SharedEquipment(record.value3(), record.value4());
        if (existing == null) {
            List<SharedEquipment> items = new ArrayList<>();
            items.add(shared);
            byUser.put(uid, new GearConnection(uid, record.value2(), items));
        } else {
            existing.sharedEquipment().add(shared);
        }
    }
}
