package app.gearaddict.equipment;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static app.gearaddict.jooq.Tables.CATALOG_AUDIT;

@Repository
public class CatalogAuditRepository {

    public enum Action { CREATE, UPDATE, DELETE }

    public enum EntityType { EQUIPMENT, MANUFACTURER }

    private final DSLContext dsl;

    public CatalogAuditRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void record(Long curatorId, Action action, EntityType entityType, Long entityId, String summary) {
        dsl.insertInto(CATALOG_AUDIT)
                .set(CATALOG_AUDIT.CURATOR_ID, curatorId)
                .set(CATALOG_AUDIT.ACTION, action.name())
                .set(CATALOG_AUDIT.ENTITY_TYPE, entityType.name())
                .set(CATALOG_AUDIT.ENTITY_ID, entityId)
                .set(CATALOG_AUDIT.SUMMARY, summary)
                .execute();
    }
}
