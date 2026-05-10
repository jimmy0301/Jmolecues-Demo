package com.example.demo.customer.domain;

import java.util.UUID;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

@ValueObject
public record CustomerId(@Identity UUID id) implements Identifier {

    public static CustomerId create() {
        return new CustomerId(UUID.randomUUID());
    }
}
