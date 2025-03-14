package com.vut.mystrategy.model;

import lombok.experimental.SuperBuilder;
import java.io.Serializable;

@SuperBuilder
public class EmaPrice extends AveragePrice implements Serializable {
    @Override
    public String toString() {
        return "EmaPrice(" + super.toString() + ")";
    }
}
