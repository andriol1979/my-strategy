package com.vut.mystrategy.model;

import java.time.Duration;

public class BarDuration {
    private final KlineIntervalEnum intervalEnum;

    public BarDuration(KlineIntervalEnum intervalEnum) {
        this.intervalEnum = intervalEnum;
    }

    public Duration getDuration() {
        return switch (intervalEnum) {
            case ONE_SECOND -> Duration.ofSeconds(1);
            case ONE_MINUTE -> Duration.ofMinutes(1);
            case THREE_MINUTES -> Duration.ofMinutes(3);
            case FIVE_MINUTES -> Duration.ofMinutes(5);
            case THIRTY_MINUTES -> Duration.ofMinutes(15);
            case ONE_HOUR -> Duration.ofHours(1);
            default -> Duration.ZERO;
        };
    }
}
