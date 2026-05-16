package com.example.demo.catalog.application;

import com.example.demo.catalog.application.command.CreateProductCommand;
import com.example.demo.catalog.domain.Product;
import com.example.demo.catalog.domain.ProductRepository;
import com.example.demo.shared.Money;
import org.jmolecules.architecture.cqrs.CommandHandler;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @CommandHandler
    public Product handle(CreateProductCommand command) {
        return productRepository.save(
                new Product(command.name(), new Money(command.amount(), command.currency())));
    }
}
