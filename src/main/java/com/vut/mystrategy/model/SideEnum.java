package com.vut.mystrategy.model;

import lombok.Getter;

@Getter
public enum SideEnum {
    SIDE_BUY("BUY"),
    SIDE_SELL("SELL");

    private final String value;

    SideEnum(String value) {
        this.value = value;
    }

    // Optional: nếu mày muốn convert từ String về enum
    public static SideEnum fromValue(String value) {
        for (SideEnum trend : SideEnum.values()) {
            if (trend.value.equalsIgnoreCase(value)) {
                return trend;
            }
        }
        throw new IllegalArgumentException("No SideEnum with value " + value);
    }
}
