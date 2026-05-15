package com.example.demo.catalog.application;

import com.example.demo.catalog.domain.Product;
import com.example.demo.catalog.domain.ProductId;
import com.example.demo.catalog.domain.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.jmolecules.architecture.cqrs.QueryModel;
import org.springframework.stereotype.Service;

@QueryModel
@Service
public class ProductQueryModel {

    private final ProductRepository productRepository;

    public ProductQueryModel(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Optional<Product> findById(ProductId id) {
        return productRepository.findById(id);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }
}
