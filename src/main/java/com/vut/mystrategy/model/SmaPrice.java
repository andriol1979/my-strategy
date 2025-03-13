package com.vut.mystrategy.model;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class SmaPrice extends AveragePrice {
    @Override
    public String toString() {
        return "SmaPrice(" + super.toString() + ")";
    }
}
