package com.example.demo.ordering.application.command;

import com.example.demo.ordering.domain.OrderId;
import org.jmolecules.architecture.cqrs.annotation.Command;

@Command
public record PlaceOrderCommand(OrderId orderId) {}
