-- V002__create_equipment_table.sql
-- EQUIPMENT entity

CREATE SEQUENCE equipment_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE equipment
(
    id           BIGINT        DEFAULT nextval('equipment_seq') PRIMARY KEY,
    name         VARCHAR(150)  NOT NULL,
    manufacturer VARCHAR(100)  NOT NULL,
    category     VARCHAR(50)   NOT NULL,
    description  VARCHAR(2000),
    CONSTRAINT equipment_name_manufacturer_unique UNIQUE (name, manufacturer),
    CONSTRAINT equipment_category_check CHECK (category IN ('Synth', 'Effect', 'Keyboard', 'Interface', 'Other'))
);
