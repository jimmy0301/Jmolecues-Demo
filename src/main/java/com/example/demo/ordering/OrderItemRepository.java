package com.example.demo.ordering;

import com.example.demo.ordering.domain.OrderItem;
import com.example.demo.ordering.domain.OrderItemId;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

// 違規示範 #2：禁止為非 AggregateRoot 的 Entity 建立 Repository
// ArchUnit 規則：@Repository must only manage types annotated with @AggregateRoot
@Repository
public interface OrderItemRepository extends MongoRepository<OrderItem, OrderItemId> {}
