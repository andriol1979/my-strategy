package com.vut.mystrategy.helper;

import java.util.List;

public class Utility {
    public static <T> boolean invalidDataList(List<T> list, int validSize) {
        return list == null || list.isEmpty() || list.size() < validSize;
    }
}
