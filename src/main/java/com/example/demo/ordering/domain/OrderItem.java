package com.example.demo.ordering.domain;

import com.example.demo.shared.Money;
import java.util.UUID;
import lombok.Getter;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.types.Entity;

@Getter
@org.jmolecules.ddd.annotation.Entity
public class OrderItem implements Entity<Order, OrderItemId> {

    @Identity private OrderItemId id = OrderItemId.create();

    private ProductReference product;

    private Quantity quantity;
    private Money unitPrice;

    protected OrderItem() {}

    public OrderItem(UUID productId, Quantity quantity, Money unitPrice) {
        this.product = new ProductReference(productId);
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}
