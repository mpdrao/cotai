package dev.cotai.devsupport.dto;

import java.time.Instant;

public record DevClock(Instant now, long offsetMinutes) {
}
