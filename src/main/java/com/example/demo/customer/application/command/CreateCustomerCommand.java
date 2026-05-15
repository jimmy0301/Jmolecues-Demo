package com.example.demo.customer.application.command;

import org.jmolecules.architecture.cqrs.Command;

@Command
public record CreateCustomerCommand(
        String name, String email, String street, String city, String postalCode, String country) {}
