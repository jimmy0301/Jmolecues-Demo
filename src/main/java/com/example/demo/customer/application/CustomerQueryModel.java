package com.example.demo.customer.application;

import com.example.demo.customer.domain.Customer;
import com.example.demo.customer.domain.CustomerId;
import com.example.demo.customer.domain.CustomerRepository;
import java.util.Optional;
import org.jmolecules.architecture.cqrs.QueryModel;
import org.springframework.stereotype.Service;

@QueryModel
@Service
public class CustomerQueryModel {

    private final CustomerRepository customerRepository;

    public CustomerQueryModel(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> findById(CustomerId id) {
        return customerRepository.findById(id);
    }
}
