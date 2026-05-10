package com.example.demo.ordering.domain;

import com.example.demo.catalog.domain.Money;
import com.example.demo.catalog.domain.ProductId;
import lombok.Getter;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.types.Entity;

@Getter
@org.jmolecules.ddd.annotation.Entity
public class OrderItem implements Entity<Order, OrderItemId> {

    @Identity private OrderItemId id = OrderItemId.create();

    private ProductId productId;
    private Quantity quantity;
    private Money unitPrice;

    protected OrderItem() {}

    public OrderItem(ProductId productId, Quantity quantity, Money unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}
