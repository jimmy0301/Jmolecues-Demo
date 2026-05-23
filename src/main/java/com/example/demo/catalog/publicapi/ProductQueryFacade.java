package com.example.demo.catalog.publicapi;

import com.example.demo.catalog.application.ProductQueryModel;
import com.example.demo.catalog.domain.ProductId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductQueryFacade {

    private final ProductQueryModel productQueryModel;

    public ProductQueryFacade(ProductQueryModel productQueryModel) {
        this.productQueryModel = productQueryModel;
    }

    public Optional<ProductSummary> findById(UUID id) {
        return productQueryModel
                .findById(new ProductId(id))
                .map(
                        product ->
                                new ProductSummary(
                                        product.getId().id(),
                                        product.getName(),
                                        product.getPrice().amount(),
                                        product.getPrice().currency()));
    }
}
