package com.example.demo.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.MongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CustomerRepositoryIT extends MongoIntegrationTest {

    @Autowired CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void saveAndFindById_preservesAllFields() {
        var address = new Address("123 Main St", "Springfield", "12345", "US");
        var customer = new Customer("Alice", "alice@example.com", address);
        customerRepository.save(customer);

        var found = customerRepository.findById(customer.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getAddress().city()).isEqualTo("Springfield");
        assertThat(found.get().getAddress().country()).isEqualTo("US");
    }

    @Test
    void findAll_afterSavingMultiple_returnsAll() {
        customerRepository.save(
                new Customer(
                        "Alice",
                        "alice@example.com",
                        new Address("1 A St", "Alpha", "11111", "US")));
        customerRepository.save(
                new Customer(
                        "Bob", "bob@example.com", new Address("2 B St", "Beta", "22222", "US")));

        assertThat(customerRepository.findAll()).hasSize(2);
    }
}
