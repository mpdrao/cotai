-- Align with the Astah object model (cotai - modelo de objeto), without changing the diagram:
--   Event -> GiftList  (- giftList 1)   every Event belongs to one list
--   Item  -> GiftList  (- giftList 1)   every Item belongs to one list
--   Event -> Event     (- event 0..1)   a list's Event points to the catalog Event it was based on
--   GiftList has no association end towards Event: the list's event is found through event.gift_list_id.
--
-- The catalog (US-01 / US-07: event types and their suggested items) becomes one template list per
-- catalog Event, owned by a system organizer that cannot log in. Catalog Events are the ones with no
-- parent (event_code IS NULL).

-- 1. A list's Event uses the list slug as its code, so the code column gets the slug's length.
ALTER TABLE event ALTER COLUMN code TYPE VARCHAR(100);
ALTER TABLE event ALTER COLUMN event_code TYPE VARCHAR(100);
ALTER TABLE item ALTER COLUMN event_code TYPE VARCHAR(100);

-- 2. System organizer that owns the catalog lists. '!' is not a BCrypt hash, so no password matches it.
INSERT INTO organizer (id, name, email, password_hash, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'Cota Ae! (catálogo)', 'catalogo@cotai.local', '!', now());

-- 3. One template list per catalog Event. DRAFT lists are never public and the system organizer has no login.
INSERT INTO gift_list (id, owner_id, event_code, title, slug, event_date, status, reservation_ttl_minutes, created_at)
SELECT gen_random_uuid(), '00000000-0000-0000-0000-000000000001', e.code, 'Catálogo: ' || e.name,
       'catalogo-' || lower(replace(e.code, '_', '-')), DATE '2099-12-31', 'DRAFT',
       e.default_reservation_ttl_minutes, now()
FROM event e
WHERE e.event_code IS NULL;

UPDATE event e
   SET gift_list_id = gl.id
  FROM gift_list gl
 WHERE gl.slug = 'catalogo-' || lower(replace(e.code, '_', '-'))
   AND e.event_code IS NULL;

-- 4. Suggested items (is_default) move into their catalog Event's template list.
UPDATE item i
   SET gift_list_id = e.gift_list_id
  FROM event e
 WHERE i.gift_list_id IS NULL
   AND i.event_code = e.code;

-- 5. Each existing organizer list gets its own Event, based on the catalog Event it used.
INSERT INTO event (code, name, default_reservation_ttl_minutes, event_code, gift_list_id)
SELECT gl.slug, e.name, e.default_reservation_ttl_minutes, e.code, gl.id
FROM gift_list gl
JOIN event e ON e.code = gl.event_code
WHERE gl.owner_id <> '00000000-0000-0000-0000-000000000001';

-- 6. GiftList no longer points to Event.
ALTER TABLE gift_list DROP COLUMN event_code;

-- 7. Multiplicity 1 on both ends towards GiftList.
ALTER TABLE event DROP CONSTRAINT fk_event_gift_list;
ALTER TABLE event ALTER COLUMN gift_list_id SET NOT NULL;
ALTER TABLE event ADD CONSTRAINT fk_event_gift_list FOREIGN KEY (gift_list_id) REFERENCES gift_list (id);
ALTER TABLE item ALTER COLUMN gift_list_id SET NOT NULL;
CREATE INDEX ix_item_event_code ON item (event_code);
