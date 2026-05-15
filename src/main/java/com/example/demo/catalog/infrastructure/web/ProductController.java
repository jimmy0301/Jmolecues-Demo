package com.example.demo.catalog.infrastructure.web;

import com.example.demo.catalog.api.ProductsApi;
import com.example.demo.catalog.api.model.Money;
import com.example.demo.catalog.api.model.ProductResponse;
import com.example.demo.catalog.application.ProductQueryModel;
import com.example.demo.catalog.domain.Product;
import com.example.demo.shared.ProductId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProductController implements ProductsApi {

    private final ProductQueryModel productQueryModel;

    ProductController(ProductQueryModel productQueryModel) {
        this.productQueryModel = productQueryModel;
    }

    @Override
    public ResponseEntity<List<ProductResponse>> getProducts() {
        var products = productQueryModel.findAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(products);
    }

    @Override
    public ResponseEntity<ProductResponse> getProductById(UUID id) {
        return productQueryModel
                .findById(new ProductId(id))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private ProductResponse toResponse(Product product) {
        var price = new Money(product.getPrice().amount(), product.getPrice().currency());
        return new ProductResponse(product.getId().id(), product.getName(), price);
    }
}
