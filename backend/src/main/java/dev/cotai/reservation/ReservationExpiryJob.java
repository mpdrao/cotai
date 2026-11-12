package dev.cotai.reservation;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final ReservationService reservations;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void run() {
        int expired = reservations.expireOverdue();
        if (expired > 0) {
            log.info("Expired {} overdue reservation(s)", expired);
        }
    }
}
