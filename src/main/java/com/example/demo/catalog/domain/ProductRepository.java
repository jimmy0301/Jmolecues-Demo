package com.example.demo.catalog.domain;

import com.example.demo.shared.ProductId;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface ProductRepository extends MongoRepository<Product, ProductId> {}
