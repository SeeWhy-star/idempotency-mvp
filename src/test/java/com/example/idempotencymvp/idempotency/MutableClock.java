package com.example.idempotencymvp.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

final class MutableClock extends Clock {

    private Instant current;
    private final ZoneId zone;

    MutableClock(Instant current) {
        this(current, ZoneId.of("UTC"));
    }

    private MutableClock(Instant current, ZoneId zone) {
        this.current = current;
        this.zone = zone;
    }

    void advance(Duration duration) {
        current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId requestedZone) {
        return new MutableClock(current, requestedZone);
    }

    @Override
    public Instant instant() {
        return current;
    }
}
