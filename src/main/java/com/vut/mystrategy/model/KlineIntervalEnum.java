package com.vut.mystrategy.model;

import lombok.Getter;

@Getter
public enum KlineIntervalEnum {

    ONE_SECOND("1s"),
    ONE_MINUTE("1m"),
    THREE_MINUTES("3m"),
    FIVE_MINUTES("5m"),
    FIFTEEN_MINUTES("15m"),
    THIRTY_MINUTES("30m"),
    ONE_HOUR("1h");

    private final String value;

    KlineIntervalEnum(String value) {
        this.value = value;
    }

    public static KlineIntervalEnum fromValue(String value) {
        for (KlineIntervalEnum trend : KlineIntervalEnum.values()) {
            if (trend.value.equalsIgnoreCase(value)) {
                return trend;
            }
        }
        throw new IllegalArgumentException("No KlineIntervalEnum with value " + value);
    }
}
