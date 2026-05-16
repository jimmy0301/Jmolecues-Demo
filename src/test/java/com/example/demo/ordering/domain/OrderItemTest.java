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
                        Quantity.of(3),
                        Money.of(new BigDecimal("20.00"), "USD"));

        var subtotal = item.subtotal();

        assertThat(subtotal.amount()).isEqualByComparingTo("60.00");
        assertThat(subtotal.currency()).isEqualTo("USD");
    }
}
