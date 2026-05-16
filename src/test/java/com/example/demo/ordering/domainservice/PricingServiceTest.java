package com.example.demo.ordering.domainservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.ordering.domain.OrderItem;
import com.example.demo.ordering.domain.Quantity;
import com.example.demo.shared.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    private OrderItem item(String amount, int qty) {
        return new OrderItem(
                UUID.randomUUID(), Quantity.of(qty), Money.of(new BigDecimal(amount), "USD"));
    }

    @Test
    void calculateTotal_singleItem_returnsSubtotal() {
        var result = pricingService.calculateTotal(List.of(item("10.00", 2)));

        assertThat(result.amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void calculateTotal_multipleItems_returnsSumOfSubtotals() {
        var result = pricingService.calculateTotal(List.of(item("10.00", 2), item("5.00", 3)));

        assertThat(result.amount()).isEqualByComparingTo("35.00");
    }

    @Test
    void calculateTotal_emptyList_throws() {
        assertThatThrownBy(() -> pricingService.calculateTotal(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }
}
