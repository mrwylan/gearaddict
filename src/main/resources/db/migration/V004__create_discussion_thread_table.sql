-- V004__create_discussion_thread_table.sql
-- DISCUSSION_THREAD entity

CREATE SEQUENCE discussion_thread_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE discussion_thread
(
    id             BIGINT         DEFAULT nextval('discussion_thread_seq') PRIMARY KEY,
    equipment_id   BIGINT         NOT NULL,
    author_id      BIGINT         NOT NULL,
    title          VARCHAR(200)   NOT NULL,
    body           VARCHAR(10000) NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_reply_at  TIMESTAMP,
    CONSTRAINT discussion_thread_equipment_fk FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE,
    CONSTRAINT discussion_thread_author_fk FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT discussion_thread_title_min_length CHECK (CHAR_LENGTH(title) >= 5),
    CONSTRAINT discussion_thread_body_min_length CHECK (CHAR_LENGTH(body) >= 10)
);

CREATE INDEX discussion_thread_equipment_idx ON discussion_thread (equipment_id);
CREATE INDEX discussion_thread_last_reply_idx ON discussion_thread (last_reply_at DESC NULLS LAST);
