-- V001__create_users_table.sql
-- USER entity (table renamed to "users" — "user" is a Postgres reserved word)

CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE users
(
    id               BIGINT       DEFAULT nextval('users_seq') PRIMARY KEY,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    email            VARCHAR(255) NOT NULL UNIQUE,
    password         VARCHAR(60)  NOT NULL,
    bio              VARCHAR(500),
    public_inventory BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX users_username_lower_idx ON users (LOWER(username));
