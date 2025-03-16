package com.vut.mystrategy.model;

import lombok.Getter;

@Getter
public enum TypeOrderEnum {
    TYPE_ORDER_LIMIT("LIMIT"),
    TYPE_ORDER_MARKET("MARKET");

    private final String value;

    TypeOrderEnum(String value) {
        this.value = value;
    }

    // Optional: nếu mày muốn convert từ String về enum
    public static TypeOrderEnum fromValue(String value) {
        for (TypeOrderEnum trend : TypeOrderEnum.values()) {
            if (trend.value.equalsIgnoreCase(value)) {
                return trend;
            }
        }
        throw new IllegalArgumentException("No TypeOrderEnum with value " + value);
    }
}
