package com.vut.mystrategy.model;

import lombok.Getter;

@Getter
public enum PositionSideEnum {
    POSITION_SIDE_LONG("LONG"),
    POSITION_SIDE_SHORT("SHORT");

    private final String value;

    PositionSideEnum(String value) {
        this.value = value;
    }

    // Optional: nếu mày muốn convert từ String về enum
    public static PositionSideEnum fromValue(String value) {
        for (PositionSideEnum trend : PositionSideEnum.values()) {
            if (trend.value.equalsIgnoreCase(value)) {
                return trend;
            }
        }
        throw new IllegalArgumentException("No PositionSideEnum with value " + value);
    }
}
