package com.example.demo.customer.domain;

import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@AggregateRoot
@Document(collection = "customers")
public class Customer {

    @Id @Identity private CustomerId id;

    private String name;
    private String email;
    private Address address;

    protected Customer() {}

    public Customer(String name, String email, Address address) {
        this.id = CustomerId.create();
        this.name = name;
        this.email = email;
        this.address = address;
    }
}
