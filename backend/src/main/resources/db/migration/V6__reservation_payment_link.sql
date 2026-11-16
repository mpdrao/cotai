-- Align reservation and payment with the object model (cotai - modelo de objeto):
-- Reservation -> Payment (0..1) and no refund status.

ALTER TABLE reservation ADD COLUMN payment_id UUID REFERENCES payment (id);

UPDATE reservation r
   SET payment_id = p.id
  FROM payment p
 WHERE p.reservation_id = r.id;

ALTER TABLE reservation ADD CONSTRAINT uq_reservation_payment UNIQUE (payment_id);

ALTER TABLE payment DROP CONSTRAINT IF EXISTS uq_payment_reservation;
ALTER TABLE payment DROP COLUMN reservation_id;

-- Late payments were flagged for refund; the money was received, so they become PAID.
UPDATE payment SET status = 'PAID' WHERE status = 'REFUND_REQUIRED';

ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_status_check;
ALTER TABLE payment ADD CONSTRAINT payment_status_check
    CHECK (status IN ('CREATED', 'PENDING', 'PAID', 'DECLINED', 'EXPIRED', 'FAILED'));
