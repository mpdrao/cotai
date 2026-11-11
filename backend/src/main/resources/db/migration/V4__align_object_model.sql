-- Align database schema with object model (cotai - modelo de objeto)

DROP TABLE IF EXISTS payment_notification;
DROP TABLE IF EXISTS item_photo;

ALTER TABLE reservation DROP CONSTRAINT IF EXISTS uq_reservation_idempotency_key;
DROP INDEX IF EXISTS ix_reservation_pending_expiry;
DROP INDEX IF EXISTS ix_reservation_item_status;

ALTER TABLE reservation DROP COLUMN IF EXISTS idempotency_key;
ALTER TABLE reservation DROP COLUMN IF EXISTS request_hash;
ALTER TABLE reservation DROP COLUMN IF EXISTS expires_at;

CREATE UNIQUE INDEX IF NOT EXISTS uq_reservation_manage_token_hash ON reservation (manage_token_hash);
CREATE INDEX IF NOT EXISTS ix_reservation_item ON reservation (item_id);

ALTER TABLE item DROP COLUMN IF EXISTS total_quotas;
ALTER TABLE item ALTER COLUMN image_url TYPE TEXT;
ALTER TABLE item ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE item ADD COLUMN IF NOT EXISTS event_type_code VARCHAR(30) REFERENCES event_type(code);
ALTER TABLE item ALTER COLUMN gift_list_id DROP NOT NULL;

