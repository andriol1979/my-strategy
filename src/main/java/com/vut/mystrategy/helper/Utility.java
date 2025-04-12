package com.vut.mystrategy.helper;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class Utility {
    public static <T> boolean invalidDataList(List<T> list, int validSize) {
        return list == null || list.isEmpty() || list.size() < validSize;
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
}
