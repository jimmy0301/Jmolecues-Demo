package com.example.demo.catalog.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.catalog.application.ProductQueryModel;
import com.example.demo.catalog.application.ProductService;
import com.example.demo.catalog.domain.Product;
import com.example.demo.shared.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ProductService productService;
    @MockBean ProductQueryModel productQueryModel;

    @Test
    void createProduct_returns201WithBody() throws Exception {
        var product = new Product("Widget", Money.of(new BigDecimal("10.00"), "USD"));
        when(productService.handle(any())).thenReturn(product);

        mockMvc.perform(
                        post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"Widget","price":{"amount":10.00,"currency":"USD"}}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price.currency").value("USD"));
    }

    @Test
    void getProducts_returns200WithList() throws Exception {
        var product = new Product("Widget", Money.of(new BigDecimal("10.00"), "USD"));
        when(productQueryModel.findAll()).thenReturn(List.of(product));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Widget"));
    }

    @Test
    void getProductById_whenFound_returns200() throws Exception {
        var product = new Product("Widget", Money.of(new BigDecimal("10.00"), "USD"));
        when(productQueryModel.findById(any())).thenReturn(Optional.of(product));

        mockMvc.perform(get("/products/{id}", product.getId().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price.amount").value(10.00));
    }

    @Test
    void getProductById_whenNotFound_returns404() throws Exception {
        when(productQueryModel.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
