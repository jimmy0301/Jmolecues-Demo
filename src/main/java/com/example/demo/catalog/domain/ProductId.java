package com.example.demo.catalog.domain;

import java.util.UUID;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

@ValueObject
public record ProductId(@Identity UUID id) implements Identifier {

    public static ProductId create() {
        return new ProductId(UUID.randomUUID());
    }
}
