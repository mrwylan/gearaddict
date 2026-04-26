-- V006__create_manufacturer_table.sql
-- Introduce MANUFACTURER as a curated reference list (UC-017) and replace the
-- free-text equipment.manufacturer column with a foreign key to it.
-- Also adds a curator flag to users so the catalog curation permission
-- (UC-017 BR-009) can be checked at sign-in.

CREATE SEQUENCE manufacturer_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE manufacturer
(
    id         BIGINT       DEFAULT nextval('manufacturer_seq') PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT manufacturer_name_min_length CHECK (CHAR_LENGTH(name) >= 2)
);

CREATE UNIQUE INDEX manufacturer_name_lower_idx ON manufacturer (LOWER(name));

-- Backfill manufacturers from existing equipment rows.
INSERT INTO manufacturer (name)
SELECT DISTINCT manufacturer
FROM equipment
WHERE manufacturer IS NOT NULL
  AND CHAR_LENGTH(manufacturer) >= 2;

ALTER TABLE equipment
    ADD COLUMN manufacturer_id BIGINT;

UPDATE equipment e
SET manufacturer_id = m.id
FROM manufacturer m
WHERE LOWER(m.name) = LOWER(e.manufacturer);

ALTER TABLE equipment
    DROP CONSTRAINT equipment_name_manufacturer_unique;

ALTER TABLE equipment
    DROP COLUMN manufacturer;

ALTER TABLE equipment
    ALTER COLUMN manufacturer_id SET NOT NULL;

ALTER TABLE equipment
    ADD CONSTRAINT equipment_manufacturer_fk
        FOREIGN KEY (manufacturer_id) REFERENCES manufacturer (id);

ALTER TABLE equipment
    ADD CONSTRAINT equipment_name_manufacturer_unique
        UNIQUE (name, manufacturer_id);

CREATE INDEX equipment_manufacturer_idx ON equipment (manufacturer_id);

-- BR-009: curator flag identifies users granted the catalog curation permission.
ALTER TABLE users
    ADD COLUMN curator BOOLEAN NOT NULL DEFAULT FALSE;

-- BR-008: audit trail of every curation action. Stores the curator id and
-- timestamp; entity_id may dangle once the referenced row is deleted, so we
-- intentionally do not enforce a foreign key.
CREATE SEQUENCE catalog_audit_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE catalog_audit
(
    id           BIGINT      DEFAULT nextval('catalog_audit_seq') PRIMARY KEY,
    curator_id   BIGINT      NOT NULL,
    action       VARCHAR(20) NOT NULL,
    entity_type  VARCHAR(20) NOT NULL,
    entity_id    BIGINT,
    summary      VARCHAR(500),
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT catalog_audit_curator_fk FOREIGN KEY (curator_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT catalog_audit_action_check CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
    CONSTRAINT catalog_audit_entity_check CHECK (entity_type IN ('EQUIPMENT', 'MANUFACTURER'))
);

CREATE INDEX catalog_audit_curator_idx ON catalog_audit (curator_id);
CREATE INDEX catalog_audit_created_at_idx ON catalog_audit (created_at DESC);
