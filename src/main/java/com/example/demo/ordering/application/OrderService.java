package com.example.demo.ordering.application;

import com.example.demo.customer.domain.CustomerId;
import com.example.demo.ordering.domain.Order;
import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderRepository;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.events = events;
    }

    public Order createOrder(CustomerId customerId) {
        return orderRepository.save(new Order(customerId));
    }

    public Order placeOrder(OrderId orderId) {
        Order order = findOrder(orderId);
        events.publishEvent(order.place());
        return orderRepository.save(order);
    }

    public Order cancelOrder(OrderId orderId) {
        Order order = findOrder(orderId);
        events.publishEvent(order.cancel());
        return orderRepository.save(order);
    }

    public Optional<Order> findById(OrderId orderId) {
        return orderRepository.findById(orderId);
    }

    private Order findOrder(OrderId orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Order not found: " + orderId.id()));
    }
}
