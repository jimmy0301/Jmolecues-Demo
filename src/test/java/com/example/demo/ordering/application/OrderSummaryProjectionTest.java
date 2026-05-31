package com.example.demo.ordering.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderPlaced;
import com.example.demo.ordering.domain.OrderStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderSummaryProjectionTest {

    @Test
    void on_orderPlaced_createsSummaryAndIgnoresDuplicateEvent() {
        var projection = new OrderSummaryProjection();
        var orderId = OrderId.create();
        var event = new OrderPlaced(orderId, Instant.parse("2026-05-31T10:15:30Z"));

        projection.on(event);
        projection.on(event);

        assertThat(projection.findById(orderId))
                .hasValue(new OrderSummary(orderId, OrderStatus.PLACED));
        assertThat(projection.findAll()).hasSize(1);
    }
}
