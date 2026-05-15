package com.example.demo.ordering.application;

import com.example.demo.ordering.domain.Order;
import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.jmolecules.architecture.cqrs.QueryModel;
import org.springframework.stereotype.Service;

@QueryModel
@Service
public class OrderQueryModel {

    private final OrderRepository orderRepository;

    public OrderQueryModel(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> findById(OrderId id) {
        return orderRepository.findById(id);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}
