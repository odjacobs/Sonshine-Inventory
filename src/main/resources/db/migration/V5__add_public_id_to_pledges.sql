ALTER TABLE pledges
    ADD COLUMN public_id CHAR(36) NULL;

UPDATE pledges
SET public_id = UUID()
WHERE public_id IS NULL;

ALTER TABLE pledges
    MODIFY public_id CHAR(36) NOT NULL;

CREATE UNIQUE INDEX uk_pledges_public_id
    ON pledges (public_id);

