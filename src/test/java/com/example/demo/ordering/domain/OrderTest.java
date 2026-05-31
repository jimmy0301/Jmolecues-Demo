package com.example.demo.ordering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.shared.Money;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Version;

class OrderTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private OrderItem anItem() {
        return new OrderItem(PRODUCT_ID, Quantity.of(1), Money.of(new BigDecimal("10.00"), "USD"));
    }

    @Test
    void newOrder_isPending() {
        var order = new Order(CUSTOMER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void addItem_whenPending_addsItem() {
        var order = new Order(CUSTOMER_ID);

        order.addItem(anItem());

        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void addItem_whenPlaced_throws() {
        var order = new Order(CUSTOMER_ID);
        order.place();

        assertThatThrownBy(() -> order.addItem(anItem()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLACED");
    }

    @Test
    void addItem_whenCancelled_throws() {
        var order = new Order(CUSTOMER_ID);
        order.cancel();

        assertThatThrownBy(() -> order.addItem(anItem()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void place_whenPending_transitionsToPlacedAndRegistersEvent() {
        var order = new Order(CUSTOMER_ID);

        order.place();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getRegisteredEvents()).hasSize(1);
        var event = (OrderPlaced) order.getRegisteredEvents().iterator().next();
        assertThat(event.orderId()).isEqualTo(order.getId());
    }

    @Test
    void place_whenAlreadyPlaced_isIdempotent() {
        var order = new Order(CUSTOMER_ID);
        order.place();

        order.place();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getRegisteredEvents()).hasSize(1);
    }

    @Test
    void cancel_whenPending_transitionsToCancelledAndRegistersEvent() {
        var order = new Order(CUSTOMER_ID);

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getRegisteredEvents()).hasSize(1);
        var event = (OrderCancelled) order.getRegisteredEvents().iterator().next();
        assertThat(event.orderId()).isEqualTo(order.getId());
    }

    @Test
    void cancel_whenPlaced_transitionsToCancelled() {
        var order = new Order(CUSTOMER_ID);
        order.place();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_whenAlreadyCancelled_throws() {
        var order = new Order(CUSTOMER_ID);
        order.cancel();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void order_hasVersionFieldForOptimisticLocking() {
        assertThat(
                        Arrays.stream(Order.class.getDeclaredFields())
                                .anyMatch(field -> field.isAnnotationPresent(Version.class)))
                .isTrue();
    }
}
