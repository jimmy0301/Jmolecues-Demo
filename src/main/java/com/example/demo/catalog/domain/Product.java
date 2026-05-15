package com.example.demo.catalog.domain;

import com.example.demo.shared.Money;
import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@AggregateRoot
@Document(collection = "products")
public class Product {

    @Id @Identity private ProductId id;

    private String name;
    private Money price;

    protected Product() {}

    public Product(String name, Money price) {
        this.id = ProductId.create();
        this.name = name;
        this.price = price;
    }
}
