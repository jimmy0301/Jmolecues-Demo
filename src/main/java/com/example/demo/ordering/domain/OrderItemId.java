package com.example.demo.ordering.domain;

import java.util.UUID;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

@ValueObject
public record OrderItemId(@Identity UUID id) implements Identifier {

    public static OrderItemId create() {
        return new OrderItemId(UUID.randomUUID());
    }
}
