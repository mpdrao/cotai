-- US-02: guests can leave a short message with their contribution; the organizer sees it on the dashboard.

ALTER TABLE reservation ADD COLUMN message VARCHAR(280);
