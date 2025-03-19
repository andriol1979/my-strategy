package com.vut.mystrategy.model;

import lombok.Getter;

@Getter
public enum PriceTrendEnum {
    UP("UP"),
    DOWN("DOWN"),
    SIDEWAYS("SIDEWAYS");

    private final String value;

    PriceTrendEnum(String value) {
        this.value = value;
    }

    // Optional: nếu mày muốn convert từ String về enum
    public static PriceTrendEnum fromValue(String value) {
        for (PriceTrendEnum trend : PriceTrendEnum.values()) {
            if (trend.value.equalsIgnoreCase(value)) {
                return trend;
            }
        }
        throw new IllegalArgumentException("No PriceTrendEnum with value " + value);
    }
}
