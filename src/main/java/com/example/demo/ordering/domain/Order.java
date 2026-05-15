package com.example.demo.ordering.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AggregateRoot
@Document(collection = "orders")
public class Order implements org.jmolecules.ddd.types.AggregateRoot<Order, OrderId> {

    @Getter @Id @Identity private OrderId id;

    private CustomerReference customer;

    private List<OrderItem> items = new ArrayList<>();

    @Getter private OrderStatus status = OrderStatus.PENDING;

    protected Order() {}

    public Order(UUID customerId) {
        this.id = OrderId.create();
        this.customer = new CustomerReference(customerId);
    }

    public CustomerReference getCustomer() {
        return customer;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(OrderItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify a " + status + " order");
        }
        items.add(item);
    }

    public OrderPlaced place() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is already " + status);
        }
        this.status = OrderStatus.PLACED;
        return new OrderPlaced(this.id, Instant.now());
    }

    public OrderCancelled cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        return new OrderCancelled(this.id, Instant.now());
    }
}
