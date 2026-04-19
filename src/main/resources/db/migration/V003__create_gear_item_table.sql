-- V003__create_gear_item_table.sql
-- GEAR_ITEM entity (links a USER's owned device to an optional EQUIPMENT catalog entry)

CREATE SEQUENCE gear_item_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE gear_item
(
    id           BIGINT        DEFAULT nextval('gear_item_seq') PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    equipment_id BIGINT,
    name         VARCHAR(150),
    category     VARCHAR(50)   NOT NULL,
    notes        VARCHAR(1000),
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT gear_item_user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT gear_item_equipment_fk FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE SET NULL,
    CONSTRAINT gear_item_category_check CHECK (category IN ('Synth', 'Effect', 'Keyboard', 'Interface', 'Other')),
    CONSTRAINT gear_item_link_or_name_check CHECK (equipment_id IS NOT NULL OR name IS NOT NULL)
);

CREATE INDEX gear_item_user_idx ON gear_item (user_id);
CREATE INDEX gear_item_equipment_idx ON gear_item (equipment_id);
