package com.vut.mystrategy.helper;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Utility {
    public static boolean isProdProfile(String activeProfile) {
        return "prod".equalsIgnoreCase(activeProfile);
    }

    public static Instant getInstantByEpochMilli(Long epochMilli) {
        return Instant.ofEpochMilli(epochMilli);
    }

    public static long getEpochMilliByInstant(Instant instant) {
        return instant.toEpochMilli();
    }

    public static ZonedDateTime getZonedDateTimeByEpochMilli(Long epochMilli) {
        Instant instant = getInstantByEpochMilli(epochMilli);
        return ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public static ZonedDateTime getZonedDateTimeByInstant(Instant instant) {
        return ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public static long getEpochMilliFromZonedDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            throw new IllegalArgumentException("zonedDateTime must not be null");
        }
        return zonedDateTime.toInstant().toEpochMilli();
    }

    public static boolean isWithinDuration(long epochMilliStart, long epochMilliEnd, long durationInMillis) {
        long diff = Math.abs(epochMilliStart - epochMilliEnd);
        return diff <= durationInMillis;
    }
}
