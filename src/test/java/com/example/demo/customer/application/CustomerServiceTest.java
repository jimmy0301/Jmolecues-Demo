package com.example.demo.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.customer.application.command.CreateCustomerCommand;
import com.example.demo.customer.domain.Customer;
import com.example.demo.customer.domain.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;

    @InjectMocks private CustomerService customerService;

    @Test
    void handle_createCustomerCommand_savesAndReturnsCustomer() {
        var command =
                new CreateCustomerCommand(
                        "Alice", "alice@example.com", "Main St", "Taipei", "10001", "TW");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = customerService.handle(command);

        verify(customerRepository).save(any(Customer.class));
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getAddress().street()).isEqualTo("Main St");
        assertThat(result.getAddress().city()).isEqualTo("Taipei");
        assertThat(result.getAddress().postalCode()).isEqualTo("10001");
        assertThat(result.getAddress().country()).isEqualTo("TW");
    }
}
