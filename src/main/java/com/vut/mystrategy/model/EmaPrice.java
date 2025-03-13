package com.vut.mystrategy.model;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class EmaPrice extends AveragePrice {
    @Override
    public String toString() {
        return "EmaPrice(" + super.toString() + ")";
    }
}
