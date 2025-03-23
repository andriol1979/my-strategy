package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.VolumeTrendEnum;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class Utility {
    public static <T> boolean invalidDataList(List<T> list, int validSize) {
        return list == null || list.isEmpty() || list.size() < validSize;
    }

    public static String concatVolumeTrendDirection(VolumeTrendEnum newVolumeTrendEnum, VolumeTrendEnum prevVolumeTrendEnum) {
        return newVolumeTrendEnum.getValue() + "->" + prevVolumeTrendEnum.getValue();
    }

    public static Instant getInstantByEpochMilli(Long epochMilli) {
        return Instant.ofEpochMilli(epochMilli);
    }

    public static ZonedDateTime getZonedDateTimeByEpochMilli(Long epochMilli) {
        Instant instant = getInstantByEpochMilli(epochMilli);
        return ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
