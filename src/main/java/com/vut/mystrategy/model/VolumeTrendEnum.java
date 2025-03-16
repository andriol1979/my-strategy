package com.vut.mystrategy.model;

import lombok.Getter;

@Getter
public enum VolumeTrendEnum {
    UP("UP"),
    DOWN("DOWN"),
    NEUTRAL("NEUTRAL");

    private final String value;

    VolumeTrendEnum(String value) {
        this.value = value;
    }

    // Optional: nếu mày muốn convert từ String về enum
    public static VolumeTrendEnum fromValue(String value) {
        for (VolumeTrendEnum trend : VolumeTrendEnum.values()) {
            if (trend.value.equalsIgnoreCase(value)) {
                return trend;
            }
        }
        throw new IllegalArgumentException("No VolumeTrend with value " + value);
    }
}
