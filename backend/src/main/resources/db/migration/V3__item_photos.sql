-- Item photos: a stock photo copied from the suggestion, or one uploaded by the organizer.
ALTER TABLE template_item ADD COLUMN image_url VARCHAR(200);
ALTER TABLE item ADD COLUMN image_url VARCHAR(200);

-- Uploaded photos. A new upload replaces the row (new id), so the public URL also works as cache key.
CREATE TABLE item_photo (
    id           UUID PRIMARY KEY,
    item_id      BIGINT       NOT NULL UNIQUE REFERENCES item (id),
    content_type VARCHAR(30)  NOT NULL,
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);
