CREATE TYPE ITEM_STATUS AS ENUM (
    'NOT_DOWNLOADED',
    'STARTED',
    'PAUSED',
    'DELETED',
    'STOPPED',
    'FAILED',
    'FINISH'
);

ALTER TABLE ITEM ADD COLUMN STATUS_TEXT VARCHAR(255);
-- noinspection SqlWithoutWhere
UPDATE ITEM SET STATUS_TEXT = STATUS;

ALTER TABLE ITEM
DROP COLUMN STATUS,
    ADD COLUMN STATUS ITEM_STATUS NOT NULL DEFAULT 'NOT_DOWNLOADED';

UPDATE ITEM
SET STATUS = STATUS_TEXT::ITEM_STATUS;

ALTER TABLE ITEM DROP COLUMN STATUS_TEXT;