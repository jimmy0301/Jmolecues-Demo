package com.example.demo.ordering.application;

import com.example.demo.ordering.domain.OrderCancelled;
import com.example.demo.ordering.domain.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        log.info("Order placed: orderId={}, at={}", event.orderId().id(), event.occurredOn());
    }

    @ApplicationModuleListener
    void on(OrderCancelled event) {
        log.info("Order cancelled: orderId={}, at={}", event.orderId().id(), event.occurredOn());
    }
}
