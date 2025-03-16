package com.vut.mystrategy.model;

import lombok.Getter;

@Getter
public enum VolumeSpikeEnum {
    BULL("BULL"),
    BEAR("BEAR"),
    FLAT("FLAT");

    private final String value;

    VolumeSpikeEnum(String value) {
        this.value = value;
    }

    // Optional: nếu mày muốn convert từ String về enum
    public static VolumeSpikeEnum fromValue(String value) {
        for (VolumeSpikeEnum trend : VolumeSpikeEnum.values()) {
            if (trend.value.equalsIgnoreCase(value)) {
                return trend;
            }
        }
        throw new IllegalArgumentException("No VolumeSpike with value " + value);
    }
}
