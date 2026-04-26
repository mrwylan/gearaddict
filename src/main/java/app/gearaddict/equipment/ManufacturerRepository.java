package app.gearaddict.equipment;

import app.gearaddict.jooq.tables.records.ManufacturerRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static app.gearaddict.jooq.Tables.EQUIPMENT;
import static app.gearaddict.jooq.Tables.MANUFACTURER;
import static org.jooq.impl.DSL.lower;

@Repository
public class ManufacturerRepository {

    private final DSLContext dsl;

    public ManufacturerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<ManufacturerRecord> findAllOrdered() {
        return dsl.selectFrom(MANUFACTURER)
                .orderBy(lower(MANUFACTURER.NAME))
                .fetch();
    }

    public Optional<ManufacturerRecord> findById(Long id) {
        return dsl.selectFrom(MANUFACTURER)
                .where(MANUFACTURER.ID.eq(id))
                .fetchOptional();
    }

    public Optional<ManufacturerRecord> findByNameIgnoreCase(String name) {
        return dsl.selectFrom(MANUFACTURER)
                .where(lower(MANUFACTURER.NAME).eq(name.toLowerCase()))
                .fetchOptional();
    }

    public boolean existsByNameIgnoreCaseExcludingId(String name, Long excludeId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(MANUFACTURER)
                        .where(lower(MANUFACTURER.NAME).eq(name.toLowerCase()))
                        .and(MANUFACTURER.ID.ne(excludeId)));
    }

    public ManufacturerRecord insert(String name) {
        return dsl.insertInto(MANUFACTURER)
                .set(MANUFACTURER.NAME, name)
                .returning()
                .fetchOne();
    }

    public ManufacturerRecord updateName(Long id, String name) {
        return dsl.update(MANUFACTURER)
                .set(MANUFACTURER.NAME, name)
                .where(MANUFACTURER.ID.eq(id))
                .returning()
                .fetchOne();
    }

    public int deleteById(Long id) {
        return dsl.deleteFrom(MANUFACTURER)
                .where(MANUFACTURER.ID.eq(id))
                .execute();
    }

    public int countEquipmentReferencing(Long manufacturerId) {
        return dsl.fetchCount(
                dsl.selectOne()
                        .from(EQUIPMENT)
                        .where(EQUIPMENT.MANUFACTURER_ID.eq(manufacturerId)));
    }
}
