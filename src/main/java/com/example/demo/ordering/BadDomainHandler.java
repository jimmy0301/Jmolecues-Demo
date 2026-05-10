package com.example.demo.ordering;

import com.example.demo.ordering.application.command.CreateOrderCommand;
import com.example.demo.ordering.domain.Order;
import com.example.demo.ordering.domain.OrderRepository;
import org.jmolecules.architecture.cqrs.CommandHandler;

/**
 * ⚠️ Violation Demo #7：@CommandHandler 出現在 domain 層（非 application 層）。
 *
 * <p>CommandHandler 屬於應用層協調邏輯，直接放在 domain 層破壞了 Onion Architecture 的邊界。
 */
public class BadDomainHandler {

    private final OrderRepository orderRepository;

    public BadDomainHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @CommandHandler
    public Order handle(CreateOrderCommand command) {
        return orderRepository.save(new Order(command.customerId()));
    }
}
