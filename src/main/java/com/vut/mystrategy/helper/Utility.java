package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.VolumeTrendEnum;

import java.util.List;

public class Utility {
    public static <T> boolean invalidDataList(List<T> list, int validSize) {
        return list == null || list.isEmpty() || list.size() < validSize;
    }

    public static String concatVolumeTrendDirection(VolumeTrendEnum newVolumeTrendEnum, VolumeTrendEnum prevVolumeTrendEnum) {
        return newVolumeTrendEnum.getValue() + "->" + prevVolumeTrendEnum.getValue();
    }
}
