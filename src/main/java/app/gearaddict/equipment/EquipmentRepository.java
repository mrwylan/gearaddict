package app.gearaddict.equipment;

import app.gearaddict.jooq.tables.records.EquipmentRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static app.gearaddict.jooq.Tables.EQUIPMENT;
import static org.jooq.impl.DSL.noCondition;

@Repository
public class EquipmentRepository {

    private final DSLContext dsl;

    public EquipmentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<EquipmentRecord> fetchPage(Optional<EquipmentCategory> category, int offset, int limit) {
        return dsl.selectFrom(EQUIPMENT)
                .where(filter(category))
                .orderBy(EQUIPMENT.MANUFACTURER.asc(), EQUIPMENT.NAME.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public int count(Optional<EquipmentCategory> category) {
        return dsl.fetchCount(dsl.selectFrom(EQUIPMENT).where(filter(category)));
    }

    private static Condition filter(Optional<EquipmentCategory> category) {
        return category.map(c -> EQUIPMENT.CATEGORY.eq(c.label()))
                .orElseGet(() -> noCondition());
    }
}
