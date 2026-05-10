package com.example.demo.ordering.application.command;

import com.example.demo.customer.domain.CustomerId;
import org.jmolecules.architecture.cqrs.annotation.Command;

@Command
public record CreateOrderCommand(CustomerId customerId) {}
