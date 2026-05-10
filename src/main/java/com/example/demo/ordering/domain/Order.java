package com.example.demo.ordering.domain;

import com.example.demo.customer.domain.CustomerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AggregateRoot
@Document(collection = "orders")
public class Order implements org.jmolecules.ddd.types.AggregateRoot<Order, OrderId> {

    @Id @Identity private OrderId id;

    private CustomerId customerId;

    private List<OrderItem> items = new ArrayList<>();

    private OrderStatus status = OrderStatus.PENDING;

    protected Order() {}

    public Order(CustomerId customerId) {
        this.id = OrderId.create();
        this.customerId = customerId;
    }

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void addItem(OrderItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify a " + status + " order");
        }
        items.add(item);
    }

    @DomainEventHandler
    public OrderPlaced place() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is already " + status);
        }
        this.status = OrderStatus.PLACED;
        return new OrderPlaced(this.id, Instant.now());
    }

    @DomainEventHandler
    public OrderCancelled cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        return new OrderCancelled(this.id, Instant.now());
    }
}
