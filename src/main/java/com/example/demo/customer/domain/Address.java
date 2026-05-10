package com.example.demo.customer.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Address(String street, String city, String postalCode, String country) {}
