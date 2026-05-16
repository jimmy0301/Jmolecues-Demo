package com.example.demo.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.catalog.application.command.CreateProductCommand;
import com.example.demo.catalog.domain.Product;
import com.example.demo.catalog.domain.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;

    @InjectMocks private ProductService productService;

    @Test
    void handle_createProductCommand_savesAndReturnsProduct() {
        var command = new CreateProductCommand("Widget", new BigDecimal("9.99"), "USD");
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = productService.handle(command);

        verify(productRepository).save(any(Product.class));
        assertThat(result.getName()).isEqualTo("Widget");
        assertThat(result.getPrice().amount()).isEqualByComparingTo("9.99");
        assertThat(result.getPrice().currency()).isEqualTo("USD");
    }
}
