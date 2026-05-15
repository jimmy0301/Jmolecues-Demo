package com.example.demo.ordering.domain;

import java.util.UUID;
import org.jmolecules.ddd.annotation.ValueObject;

/** ordering context 對顧客的參照。不實作 Identifier，避免 ByteBuddy 將此 field 誤標為 @Id。 */
@ValueObject
public record CustomerReference(UUID id) {}
