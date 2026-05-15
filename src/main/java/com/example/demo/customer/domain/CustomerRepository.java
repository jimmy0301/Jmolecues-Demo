package com.example.demo.customer.domain;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, CustomerId> {}
