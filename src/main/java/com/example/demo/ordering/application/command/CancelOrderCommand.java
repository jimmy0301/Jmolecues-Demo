package com.example.demo.ordering.application.command;

import com.example.demo.ordering.domain.OrderId;
import org.jmolecules.architecture.cqrs.Command;

@Command
public record CancelOrderCommand(OrderId orderId) {}
