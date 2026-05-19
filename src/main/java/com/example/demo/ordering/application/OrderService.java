package com.example.demo.ordering.application;

import com.example.demo.ordering.application.command.CancelOrderCommand;
import com.example.demo.ordering.application.command.CreateOrderCommand;
import com.example.demo.ordering.application.command.PlaceOrderCommand;
import com.example.demo.ordering.domain.Order;
import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderRepository;
import java.util.Optional;
import org.jmolecules.architecture.cqrs.CommandHandler;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @CommandHandler
    public Order handle(CreateOrderCommand command) {
        return orderRepository.save(new Order(command.customerId()));
    }

    @CommandHandler
    public Order handle(PlaceOrderCommand command) {
        Order order = findOrder(command.orderId());
        order.place();
        return orderRepository.save(order);
    }

    @CommandHandler
    public Order handle(CancelOrderCommand command) {
        Order order = findOrder(command.orderId());
        order.cancel();
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
