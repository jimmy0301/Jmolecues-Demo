package com.example.demo.ordering.application;

import com.example.demo.ordering.domain.OrderCancelled;
import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderPlaced;
import com.example.demo.ordering.domain.OrderStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jmolecules.architecture.cqrs.QueryModel;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@QueryModel
@Component
public class OrderSummaryProjection {

    private final Map<OrderId, OrderSummary> summaries = new LinkedHashMap<>();

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        summaries.put(event.orderId(), new OrderSummary(event.orderId(), OrderStatus.PLACED));
    }

    @ApplicationModuleListener
    void on(OrderCancelled event) {
        summaries.put(event.orderId(), new OrderSummary(event.orderId(), OrderStatus.CANCELLED));
    }

    public Optional<OrderSummary> findById(OrderId orderId) {
        return Optional.ofNullable(summaries.get(orderId));
    }

    public List<OrderSummary> findAll() {
        return new ArrayList<>(summaries.values());
    }
}
