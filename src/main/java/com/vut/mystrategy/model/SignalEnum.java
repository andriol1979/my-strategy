package com.vut.mystrategy.model;

import lombok.Getter;

@Getter
public enum SignalEnum {

    ENTRY("ENTRY"),
    EXIT("EXIT");

    private final String value;

    SignalEnum(String value) {
        this.value = value;
    }

    // Optional: nếu mày muốn convert từ String về enum
    public static SignalEnum fromValue(String value) {
        for (SignalEnum trend : SignalEnum.values()) {
            if (trend.value.equalsIgnoreCase(value)) {
                return trend;
            }
        }
        throw new IllegalArgumentException("No SignalEnum with value " + value);
    }
}
