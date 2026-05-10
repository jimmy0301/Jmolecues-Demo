package com.example.demo.ordering.domain;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface OrderRepository extends MongoRepository<Order, OrderId> {}
