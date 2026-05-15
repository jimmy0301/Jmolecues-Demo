package com.example.demo.customer.infrastructure.web;

import com.example.demo.customer.api.CustomersApi;
import com.example.demo.customer.api.model.AddressDto;
import com.example.demo.customer.api.model.CreateCustomerRequest;
import com.example.demo.customer.api.model.CustomerResponse;
import com.example.demo.customer.application.CustomerQueryModel;
import com.example.demo.customer.application.CustomerService;
import com.example.demo.customer.application.command.CreateCustomerCommand;
import com.example.demo.customer.domain.Customer;
import com.example.demo.shared.CustomerId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CustomerController implements CustomersApi {

    private final CustomerQueryModel customerQueryModel;
    private final CustomerService customerService;

    CustomerController(CustomerQueryModel customerQueryModel, CustomerService customerService) {
        this.customerQueryModel = customerQueryModel;
        this.customerService = customerService;
    }

    @Override
    public ResponseEntity<CustomerResponse> getCustomerById(UUID id) {
        return customerQueryModel
                .findById(new CustomerId(id))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<CustomerResponse> createCustomer(
            CreateCustomerRequest createCustomerRequest) {
        var command =
                new CreateCustomerCommand(
                        createCustomerRequest.getName(),
                        createCustomerRequest.getEmail(),
                        createCustomerRequest.getAddress().getStreet(),
                        createCustomerRequest.getAddress().getCity(),
                        createCustomerRequest.getAddress().getPostalCode(),
                        createCustomerRequest.getAddress().getCountry());
        var customer = customerService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(customer));
    }

    private CustomerResponse toResponse(Customer customer) {
        var address =
                new AddressDto(
                        customer.getAddress().street(),
                        customer.getAddress().city(),
                        customer.getAddress().postalCode(),
                        customer.getAddress().country());
        return new CustomerResponse(
                customer.getId().id(), customer.getName(), customer.getEmail(), address);
    }
}
