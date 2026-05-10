package com.example.demo.ordering.domain;

import java.util.UUID;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

@ValueObject
public record OrderId(@Identity UUID id) implements Identifier {

    public static OrderId create() {
        return new OrderId(UUID.randomUUID());
    }
}
