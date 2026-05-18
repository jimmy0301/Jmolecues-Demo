package com.example.demo.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.MongoIntegrationTest;
import com.example.demo.shared.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProductRepositoryIT extends MongoIntegrationTest {

    @Autowired ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void saveAndFindById_preservesNameAndPrice() {
        var product = new Product("Widget", Money.of(new BigDecimal("10.00"), "USD"));
        productRepository.save(product);

        var found = productRepository.findById(product.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Widget");
        assertThat(found.get().getPrice().amount()).isEqualByComparingTo("10.00");
        assertThat(found.get().getPrice().currency()).isEqualTo("USD");
    }

    @Test
    void findAll_afterSavingMultiple_returnsAll() {
        productRepository.save(new Product("Widget", Money.of(new BigDecimal("10.00"), "USD")));
        productRepository.save(new Product("Gadget", Money.of(new BigDecimal("20.00"), "USD")));

        assertThat(productRepository.findAll()).hasSize(2);
    }

    @Test
    void deleteById_removesProduct() {
        var product = new Product("Widget", Money.of(new BigDecimal("10.00"), "USD"));
        productRepository.save(product);

        productRepository.deleteById(product.getId());

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }
}
