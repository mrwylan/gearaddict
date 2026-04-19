-- V005__create_reply_table.sql
-- REPLY entity

CREATE SEQUENCE reply_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE reply
(
    id                   BIGINT         DEFAULT nextval('reply_seq') PRIMARY KEY,
    discussion_thread_id BIGINT         NOT NULL,
    author_id            BIGINT         NOT NULL,
    body                 VARCHAR(10000) NOT NULL,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reply_thread_fk FOREIGN KEY (discussion_thread_id) REFERENCES discussion_thread (id) ON DELETE CASCADE,
    CONSTRAINT reply_author_fk FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT reply_body_min_length CHECK (CHAR_LENGTH(body) >= 1)
);

CREATE INDEX reply_thread_idx ON reply (discussion_thread_id);
