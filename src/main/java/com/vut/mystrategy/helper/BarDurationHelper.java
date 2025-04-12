package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.KlineIntervalEnum;
import java.time.Duration;
import static com.vut.mystrategy.model.KlineIntervalEnum.*;

public class BarDurationHelper {
    public static Duration getDuration(KlineIntervalEnum intervalEnum) {
        return switch (intervalEnum) {
            case ONE_SECOND -> Duration.ofSeconds(1);
            case ONE_MINUTE -> Duration.ofMinutes(1);
            case THREE_MINUTES -> Duration.ofMinutes(3);
            case FIVE_MINUTES -> Duration.ofMinutes(5);
            case FIFTEEN_MINUTES -> Duration.ofMinutes(15);
            case THIRTY_MINUTES -> Duration.ofMinutes(30);
            case ONE_HOUR -> Duration.ofHours(1);
            default -> Duration.ZERO;
        };
    }

    public static KlineIntervalEnum getEnumFromDuration(Duration duration) {
        if(Duration.ofSeconds(1).compareTo(duration) == 0) {
            return ONE_SECOND;
        }
        if(Duration.ofMinutes(1).compareTo(duration) == 0) {
            return ONE_MINUTE;
        }
        if(Duration.ofMinutes(3).compareTo(duration) == 0) {
            return THREE_MINUTES;
        }
        if(Duration.ofMinutes(5).compareTo(duration) == 0) {
            return FIVE_MINUTES;
        }
        if(Duration.ofMinutes(15).compareTo(duration) == 0) {
            return FIFTEEN_MINUTES;
        }
        if(Duration.ofMinutes(30).compareTo(duration) == 0) {
            return THIRTY_MINUTES;
        }
        if(Duration.ofHours(1).compareTo(duration) == 0) {
            return ONE_HOUR;
        }

        throw new RuntimeException("Invalid duration: " + duration);
    }
}
