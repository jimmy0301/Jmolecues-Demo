package com.example.demo.ordering.application.command;

import com.example.demo.shared.CustomerId;
import org.jmolecules.architecture.cqrs.Command;

@Command
public record CreateOrderCommand(CustomerId customerId) {}
