package com.example.demo.ordering;

import com.example.demo.customer.domain.Customer;
import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderStatus;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// 違規示範 #1：禁止跨 Bounded Context 直接持有 Aggregate 物件
// ArchUnit 規則：fields of AggregateRoot type should use id reference or Association
@AggregateRoot
@Document(collection = "bad_orders")
public class BadOrder {

    @Id @Identity private OrderId id;

    // 違規：直接持有 Customer 物件，應只持有 CustomerId
    private Customer customer;

    private OrderStatus status = OrderStatus.PENDING;

    protected BadOrder() {}

    public OrderId getId() {
        return id;
    }
}
