package app.gearaddict.equipment;

import app.gearaddict.jooq.tables.records.EquipmentRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static app.gearaddict.jooq.Tables.EQUIPMENT;
import static app.gearaddict.jooq.Tables.GEAR_ITEM;
import static org.jooq.impl.DSL.noCondition;

@Repository
public class EquipmentRepository {

    private final DSLContext dsl;

    public EquipmentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<EquipmentRecord> fetchPage(Optional<EquipmentCategory> category,
                                           Optional<String> searchTerm,
                                           int offset,
                                           int limit) {
        return dsl.selectFrom(EQUIPMENT)
                .where(filter(category, searchTerm))
                .orderBy(EQUIPMENT.MANUFACTURER.asc(), EQUIPMENT.NAME.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public int count(Optional<EquipmentCategory> category, Optional<String> searchTerm) {
        return dsl.fetchCount(dsl.selectFrom(EQUIPMENT).where(filter(category, searchTerm)));
    }

    public Optional<EquipmentRecord> findById(Long id) {
        return dsl.selectFrom(EQUIPMENT)
                .where(EQUIPMENT.ID.eq(id))
                .fetchOptional();
    }

    public int countOwners(Long equipmentId) {
        return dsl.fetchCount(
                dsl.selectFrom(GEAR_ITEM)
                        .where(GEAR_ITEM.EQUIPMENT_ID.eq(equipmentId)));
    }

    private static Condition filter(Optional<EquipmentCategory> category, Optional<String> searchTerm) {
        Condition condition = noCondition();
        if (category.isPresent()) {
            condition = condition.and(EQUIPMENT.CATEGORY.eq(category.get().label()));
        }
        if (searchTerm.isPresent()) {
            String like = "%" + searchTerm.get().trim() + "%";
            condition = condition.and(
                    EQUIPMENT.NAME.likeIgnoreCase(like)
                            .or(EQUIPMENT.MANUFACTURER.likeIgnoreCase(like)));
        }
        return condition;
    }
}
