package com.example.demo.customer.application;

import com.example.demo.customer.application.command.CreateCustomerCommand;
import com.example.demo.customer.domain.Address;
import com.example.demo.customer.domain.Customer;
import com.example.demo.customer.domain.CustomerRepository;
import org.jmolecules.architecture.cqrs.CommandHandler;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @CommandHandler
    public Customer handle(CreateCustomerCommand command) {
        var customer =
                new Customer(
                        command.name(),
                        command.email(),
                        new Address(
                                command.street(),
                                command.city(),
                                command.postalCode(),
                                command.country()));
        return customerRepository.save(customer);
    }
}
