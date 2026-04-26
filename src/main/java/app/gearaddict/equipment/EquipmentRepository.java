package app.gearaddict.equipment;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record6;
import org.jooq.SelectOnConditionStep;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static app.gearaddict.jooq.Tables.EQUIPMENT;
import static app.gearaddict.jooq.Tables.GEAR_ITEM;
import static app.gearaddict.jooq.Tables.MANUFACTURER;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.noCondition;

@Repository
public class EquipmentRepository {

    private final DSLContext dsl;

    public EquipmentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Equipment> fetchPage(Optional<EquipmentCategory> category,
                                     Optional<String> searchTerm,
                                     int offset,
                                     int limit) {
        return baseSelect()
                .where(filter(category, searchTerm))
                .orderBy(lower(MANUFACTURER.NAME), lower(EQUIPMENT.NAME))
                .offset(offset)
                .limit(limit)
                .fetch(EquipmentRepository::toDomain);
    }

    public int count(Optional<EquipmentCategory> category, Optional<String> searchTerm) {
        return dsl.fetchCount(
                dsl.selectOne()
                        .from(EQUIPMENT)
                        .join(MANUFACTURER).on(MANUFACTURER.ID.eq(EQUIPMENT.MANUFACTURER_ID))
                        .where(filter(category, searchTerm)));
    }

    public Optional<Equipment> findById(Long id) {
        return baseSelect()
                .where(EQUIPMENT.ID.eq(id))
                .fetchOptional(EquipmentRepository::toDomain);
    }

    public Optional<Equipment> findByNameAndManufacturer(String name, Long manufacturerId) {
        return baseSelect()
                .where(lower(EQUIPMENT.NAME).eq(name.toLowerCase()))
                .and(EQUIPMENT.MANUFACTURER_ID.eq(manufacturerId))
                .fetchOptional(EquipmentRepository::toDomain);
    }

    public boolean existsByNameAndManufacturerExcludingId(String name, Long manufacturerId, Long excludeId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(EQUIPMENT)
                        .where(lower(EQUIPMENT.NAME).eq(name.toLowerCase()))
                        .and(EQUIPMENT.MANUFACTURER_ID.eq(manufacturerId))
                        .and(EQUIPMENT.ID.ne(excludeId)));
    }

    public int countOwners(Long equipmentId) {
        return dsl.fetchCount(
                dsl.selectFrom(GEAR_ITEM)
                        .where(GEAR_ITEM.EQUIPMENT_ID.eq(equipmentId)));
    }

    public Long insert(String name, Long manufacturerId, EquipmentCategory category, String description) {
        return dsl.insertInto(EQUIPMENT)
                .set(EQUIPMENT.NAME, name)
                .set(EQUIPMENT.MANUFACTURER_ID, manufacturerId)
                .set(EQUIPMENT.CATEGORY, category.label())
                .set(EQUIPMENT.DESCRIPTION, description)
                .returning(EQUIPMENT.ID)
                .fetchOne()
                .getId();
    }

    public void update(Long id, String name, Long manufacturerId, EquipmentCategory category, String description) {
        dsl.update(EQUIPMENT)
                .set(EQUIPMENT.NAME, name)
                .set(EQUIPMENT.MANUFACTURER_ID, manufacturerId)
                .set(EQUIPMENT.CATEGORY, category.label())
                .set(EQUIPMENT.DESCRIPTION, description)
                .where(EQUIPMENT.ID.eq(id))
                .execute();
    }

    public int deleteById(Long id) {
        return dsl.deleteFrom(EQUIPMENT)
                .where(EQUIPMENT.ID.eq(id))
                .execute();
    }

    /**
     * Detaches gear items from an equipment entry by copying the catalog values
     * into the gear_item.name free-text column so user inventory data survives
     * deletion (UC-017 BR-004).
     */
    public int unlinkGearItemsFromEquipment(Long equipmentId) {
        return dsl.update(GEAR_ITEM)
                .set(GEAR_ITEM.NAME,
                        dsl.select(MANUFACTURER.NAME.concat(" ").concat(EQUIPMENT.NAME))
                                .from(EQUIPMENT)
                                .join(MANUFACTURER).on(MANUFACTURER.ID.eq(EQUIPMENT.MANUFACTURER_ID))
                                .where(EQUIPMENT.ID.eq(equipmentId)))
                .set(GEAR_ITEM.EQUIPMENT_ID, (Long) null)
                .where(GEAR_ITEM.EQUIPMENT_ID.eq(equipmentId))
                .execute();
    }

    private SelectOnConditionStep<Record6<Long, String, Long, String, String, String>> baseSelect() {
        return dsl.select(
                        EQUIPMENT.ID,
                        EQUIPMENT.NAME,
                        EQUIPMENT.MANUFACTURER_ID,
                        MANUFACTURER.NAME,
                        EQUIPMENT.CATEGORY,
                        EQUIPMENT.DESCRIPTION)
                .from(EQUIPMENT)
                .join(MANUFACTURER).on(MANUFACTURER.ID.eq(EQUIPMENT.MANUFACTURER_ID));
    }

    private Condition filter(Optional<EquipmentCategory> category, Optional<String> searchTerm) {
        Condition condition = noCondition();
        if (category.isPresent()) {
            condition = condition.and(EQUIPMENT.CATEGORY.eq(category.get().label()));
        }
        if (searchTerm.isPresent()) {
            String like = "%" + searchTerm.get().trim() + "%";
            condition = condition.and(
                    EQUIPMENT.NAME.likeIgnoreCase(like)
                            .or(MANUFACTURER.NAME.likeIgnoreCase(like)));
        }
        return condition;
    }

    private static Equipment toDomain(Record record) {
        return new Equipment(
                record.get(EQUIPMENT.ID),
                record.get(EQUIPMENT.NAME),
                record.get(EQUIPMENT.MANUFACTURER_ID),
                record.get(MANUFACTURER.NAME),
                EquipmentCategory.fromLabel(record.get(EQUIPMENT.CATEGORY)).orElse(EquipmentCategory.OTHER),
                record.get(EQUIPMENT.DESCRIPTION));
    }
}
