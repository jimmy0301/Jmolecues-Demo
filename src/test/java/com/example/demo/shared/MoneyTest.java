package com.example.demo.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void add_sameCurrency_returnsSum() {
        var a = Money.of(new BigDecimal("10.00"), "USD");
        var b = Money.of(new BigDecimal("5.50"), "USD");

        var result = a.add(b);

        assertThat(result.amount()).isEqualByComparingTo("15.50");
        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    void add_differentCurrency_throws() {
        var usd = Money.of(new BigDecimal("10.00"), "USD");
        var twd = Money.of(new BigDecimal("300.00"), "TWD");

        assertThatThrownBy(() -> usd.add(twd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    void multiply_returnsScaledAmount() {
        var price = Money.of(new BigDecimal("25.00"), "USD");

        var result = price.multiply(3);

        assertThat(result.amount()).isEqualByComparingTo("75.00");
        assertThat(result.currency()).isEqualTo("USD");
    }
}
