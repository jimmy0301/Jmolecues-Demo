package com.example.demo.ordering.application.command;

import java.util.UUID;
import org.jmolecules.architecture.cqrs.Command;

@Command
public record CreateOrderCommand(UUID customerId) {}
