package com.example.demo.ordering.domainservice;

import com.example.demo.ordering.domain.OrderItem;
import com.example.demo.shared.Money;
import java.util.List;
import org.jmolecules.ddd.annotation.Service;

@Service
public class PricingService {

    public Money calculateTotal(List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cannot calculate total for empty items");
        }
        return items.stream().map(OrderItem::subtotal).reduce(Money::add).orElseThrow();
    }
}
