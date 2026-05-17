package com.example.demo.customer.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.customer.application.CustomerQueryModel;
import com.example.demo.customer.application.CustomerService;
import com.example.demo.customer.domain.Address;
import com.example.demo.customer.domain.Customer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CustomerService customerService;
    @MockBean CustomerQueryModel customerQueryModel;

    @Test
    void createCustomer_returns201WithBody() throws Exception {
        var customer =
                new Customer(
                        "Alice",
                        "alice@example.com",
                        new Address("123 Main St", "Taipei", "10001", "TW"));
        when(customerService.handle(any())).thenReturn(customer);

        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Alice",
                                          "email": "alice@example.com",
                                          "address": {
                                            "street": "123 Main St",
                                            "city": "Taipei",
                                            "postalCode": "10001",
                                            "country": "TW"
                                          }
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.address.city").value("Taipei"));
    }

    @Test
    void getCustomerById_whenFound_returns200() throws Exception {
        var customer =
                new Customer(
                        "Alice",
                        "alice@example.com",
                        new Address("123 Main St", "Taipei", "10001", "TW"));
        when(customerQueryModel.findById(any())).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/customers/{id}", customer.getId().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void getCustomerById_whenNotFound_returns404() throws Exception {
        when(customerQueryModel.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/customers/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
