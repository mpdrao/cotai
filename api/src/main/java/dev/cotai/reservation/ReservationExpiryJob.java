package dev.cotai.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** UC18. Every 60 s. Idempotent, so running on several instances at once is harmless (EC16). */
@Component
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final ReservationService reservations;

    public ReservationExpiryJob(ReservationService reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void run() {
        int expired = reservations.expireOverdue();
        if (expired > 0) {
            log.info("Expired {} overdue reservation(s)", expired);
        }
    }
}
