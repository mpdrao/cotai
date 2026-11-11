package dev.cotai.devsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

    private final Clock base;
    private volatile Duration offset = Duration.ZERO;

    public MutableClock(Clock base) {
        this.base = base;
    }

    public void advance(Duration amount) {
        offset = offset.plus(amount);
    }

    public void reset() {
        offset = Duration.ZERO;
    }

    public long offsetMinutes() {
        return offset.toMinutes();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return base.instant().plus(offset);
    }
}
