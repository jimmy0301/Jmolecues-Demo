package com.example.demo.ordering.domain;

import java.time.Instant;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record OrderCancelled(OrderId orderId, Instant occurredOn) {}
