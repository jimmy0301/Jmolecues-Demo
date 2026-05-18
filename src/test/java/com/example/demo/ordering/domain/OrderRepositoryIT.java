package com.example.demo.ordering.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.MongoIntegrationTest;
import com.example.demo.shared.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderRepositoryIT extends MongoIntegrationTest {

    @Autowired OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void saveAndFindById_preservesCustomerAndStatus() {
        var customerId = UUID.randomUUID();
        var order = new Order(customerId);
        orderRepository.save(order);

        var found = orderRepository.findById(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(found.get().getCustomer().id()).isEqualTo(customerId);
    }

    @Test
    void saveAndFindById_preservesOrderItems() {
        var order = new Order(UUID.randomUUID());
        order.addItem(
                new OrderItem(
                        UUID.randomUUID(),
                        Quantity.of(2),
                        Money.of(new BigDecimal("15.00"), "USD")));
        orderRepository.save(order);

        var found = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getQuantity().value()).isEqualTo(2);
        assertThat(found.getItems().get(0).getUnitPrice().amount()).isEqualByComparingTo("15.00");
    }

    @Test
    void save_afterPlace_persistsPlacedStatus() {
        var order = new Order(UUID.randomUUID());
        order.place();
        orderRepository.save(order);

        var found = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void save_afterCancel_persistsCancelledStatus() {
        var order = new Order(UUID.randomUUID());
        order.cancel();
        orderRepository.save(order);

        var found = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
