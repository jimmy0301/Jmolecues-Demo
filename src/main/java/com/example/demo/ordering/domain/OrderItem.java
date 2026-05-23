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
    private String productNameSnapshot;
    private Money unitPriceSnapshot;

    protected OrderItem() {}

    public OrderItem(UUID productId, Quantity quantity, Money unitPrice) {
        this(productId, null, quantity, unitPrice);
    }

    public OrderItem(UUID productId, String productName, Quantity quantity, Money unitPrice) {
        this.product = new ProductReference(productId);
        this.productNameSnapshot = productName;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPrice;
    }

    public Money subtotal() {
        return unitPriceSnapshot.multiply(quantity.value());
    }

    public Money getUnitPrice() {
        return unitPriceSnapshot;
    }
}
