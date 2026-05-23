package com.example.demo.ordering.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.shared.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    @Test
    void subtotal_returnsUnitPriceMultipliedByQuantity() {
        var item =
                new OrderItem(
                        UUID.randomUUID(),
                        "Widget",
                        Quantity.of(3),
                        Money.of(new BigDecimal("20.00"), "USD"));

        var subtotal = item.subtotal();

        assertThat(subtotal.amount()).isEqualByComparingTo("60.00");
        assertThat(subtotal.currency()).isEqualTo("USD");
    }

    @Test
    void constructor_preservesProductNameAndUnitPriceSnapshots() {
        var productId = UUID.randomUUID();
        var unitPrice = Money.of(new BigDecimal("12.50"), "USD");

        var item = new OrderItem(productId, "Widget", Quantity.of(2), unitPrice);

        assertThat(item.getProduct().id()).isEqualTo(productId);
        assertThat(item.getProductNameSnapshot()).isEqualTo("Widget");
        assertThat(item.getUnitPriceSnapshot()).isEqualTo(unitPrice);
    }
}
