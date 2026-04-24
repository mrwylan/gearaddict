package app.gearaddict.gear;

import app.gearaddict.equipment.EquipmentCategory;
import org.jooq.DSLContext;
import org.jooq.Record9;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static app.gearaddict.jooq.Tables.EQUIPMENT;
import static app.gearaddict.jooq.Tables.GEAR_ITEM;

@Repository
public class GearItemRepository {

    private final DSLContext dsl;

    public GearItemRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<GearItem> fetchForUser(Long userId, Optional<EquipmentCategory> category, int offset, int limit) {
        return selectEnriched()
                .where(GEAR_ITEM.USER_ID.eq(userId))
                .and(category.map(c -> GEAR_ITEM.CATEGORY.eq(c.label()))
                        .orElse(org.jooq.impl.DSL.noCondition()))
                .orderBy(org.jooq.impl.DSL.coalesce(EQUIPMENT.NAME, GEAR_ITEM.NAME).asc())
                .offset(offset)
                .limit(limit)
                .fetch(GearItemRepository::toDomain);
    }

    public int countForUser(Long userId, Optional<EquipmentCategory> category) {
        return dsl.fetchCount(
                dsl.selectFrom(GEAR_ITEM)
                        .where(GEAR_ITEM.USER_ID.eq(userId))
                        .and(category.map(c -> GEAR_ITEM.CATEGORY.eq(c.label()))
                                .orElse(org.jooq.impl.DSL.noCondition())));
    }

    public Optional<GearItem> findById(Long id) {
        return selectEnriched()
                .where(GEAR_ITEM.ID.eq(id))
                .fetchOptional(GearItemRepository::toDomain);
    }

    public boolean existsForUserWithEquipment(Long userId, Long equipmentId) {
        return dsl.fetchExists(
                dsl.selectFrom(GEAR_ITEM)
                        .where(GEAR_ITEM.USER_ID.eq(userId))
                        .and(GEAR_ITEM.EQUIPMENT_ID.eq(equipmentId)));
    }

    public boolean existsForUserWithName(Long userId, String name) {
        return dsl.fetchExists(
                dsl.selectFrom(GEAR_ITEM)
                        .where(GEAR_ITEM.USER_ID.eq(userId))
                        .and(GEAR_ITEM.EQUIPMENT_ID.isNull())
                        .and(org.jooq.impl.DSL.lower(GEAR_ITEM.NAME).eq(name.toLowerCase())));
    }

    public Long insert(Long userId, Long equipmentId, String name, EquipmentCategory category, String notes) {
        return dsl.insertInto(GEAR_ITEM)
                .set(GEAR_ITEM.USER_ID, userId)
                .set(GEAR_ITEM.EQUIPMENT_ID, equipmentId)
                .set(GEAR_ITEM.NAME, name)
                .set(GEAR_ITEM.CATEGORY, category.label())
                .set(GEAR_ITEM.NOTES, notes)
                .returning(GEAR_ITEM.ID)
                .fetchOne()
                .getId();
    }

    public void update(Long id, Long equipmentId, String name, EquipmentCategory category, String notes) {
        dsl.update(GEAR_ITEM)
                .set(GEAR_ITEM.EQUIPMENT_ID, equipmentId)
                .set(GEAR_ITEM.NAME, name)
                .set(GEAR_ITEM.CATEGORY, category.label())
                .set(GEAR_ITEM.NOTES, notes)
                .where(GEAR_ITEM.ID.eq(id))
                .execute();
    }

    public void deleteById(Long id) {
        dsl.deleteFrom(GEAR_ITEM)
                .where(GEAR_ITEM.ID.eq(id))
                .execute();
    }

    private org.jooq.SelectJoinStep<Record9<Long, Long, Long, String, String, String, String, String, LocalDateTime>> selectEnriched() {
        return dsl.select(
                        GEAR_ITEM.ID,
                        GEAR_ITEM.USER_ID,
                        GEAR_ITEM.EQUIPMENT_ID,
                        EQUIPMENT.NAME,
                        EQUIPMENT.MANUFACTURER,
                        GEAR_ITEM.NAME,
                        GEAR_ITEM.CATEGORY,
                        GEAR_ITEM.NOTES,
                        GEAR_ITEM.CREATED_AT)
                .from(GEAR_ITEM)
                .leftJoin(EQUIPMENT).on(EQUIPMENT.ID.eq(GEAR_ITEM.EQUIPMENT_ID));
    }

    private static GearItem toDomain(Record9<Long, Long, Long, String, String, String, String, String, LocalDateTime> r) {
        return new GearItem(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6(),
                EquipmentCategory.fromLabel(r.value7()).orElse(EquipmentCategory.OTHER),
                r.value8(),
                r.value9());
    }
}
