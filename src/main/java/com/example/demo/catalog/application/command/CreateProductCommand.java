package com.example.demo.catalog.application.command;

import java.math.BigDecimal;
import org.jmolecules.architecture.cqrs.Command;

@Command
public record CreateProductCommand(String name, BigDecimal amount, String currency) {}
