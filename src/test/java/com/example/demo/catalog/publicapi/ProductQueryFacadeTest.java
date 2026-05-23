package com.example.demo.catalog.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.catalog.application.ProductQueryModel;
import com.example.demo.catalog.domain.Product;
import com.example.demo.catalog.domain.ProductId;
import com.example.demo.shared.Money;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductQueryFacadeTest {

    @Test
    void findById_mapsProductToPublicSummary() {
        var product = new Product("Widget", Money.of(new BigDecimal("9.99"), "USD"));
        var productQueryModel = new StubProductQueryModel(product.getId(), product);
        var facade = new ProductQueryFacade(productQueryModel);

        var summary = facade.findById(product.getId().id());

        assertThat(summary).isPresent();
        assertThat(summary.get().id()).isEqualTo(product.getId().id());
        assertThat(summary.get().name()).isEqualTo("Widget");
        assertThat(summary.get().amount()).isEqualByComparingTo("9.99");
        assertThat(summary.get().currency()).isEqualTo("USD");
    }

    @Test
    void findById_whenProductMissing_returnsEmpty() {
        var productId = UUID.randomUUID();
        var productQueryModel = new StubProductQueryModel(new ProductId(productId), null);
        var facade = new ProductQueryFacade(productQueryModel);

        var summary = facade.findById(productId);

        assertThat(summary).isEmpty();
    }

    private static final class StubProductQueryModel extends ProductQueryModel {

        private final ProductId expectedId;
        private final Product product;

        private StubProductQueryModel(ProductId expectedId, Product product) {
            super(null);
            this.expectedId = expectedId;
            this.product = product;
        }

        @Override
        public Optional<Product> findById(ProductId id) {
            if (!expectedId.equals(id) || product == null) {
                return Optional.empty();
            }
            return Optional.of(product);
        }
    }
}
