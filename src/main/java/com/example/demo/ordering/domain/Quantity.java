package com.example.demo.ordering.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Quantity(int value) {

    public Quantity {
        if (value <= 0) throw new IllegalArgumentException("Quantity must be positive");
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }
}
