package com.example.demo.ordering;

import com.example.demo.ordering.application.OrderService;
import com.example.demo.ordering.application.command.CreateOrderCommand;
import com.example.demo.ordering.domain.Order;
import java.util.UUID;
import org.jmolecules.architecture.cqrs.QueryModel;

/**
 * ⚠️ Violation Demo #5：@QueryModel 直接呼叫 @CommandHandler，造成 read side 觸發 state change。
 *
 * <p>CQRS 核心規則：Query 只能讀取狀態，不可改變狀態。
 */
@QueryModel
public class BadQueryModel {

    private final OrderService orderService;

    public BadQueryModel(OrderService orderService) {
        this.orderService = orderService;
    }

    public Order findOrCreate(UUID customerId) {
        // 違規：query 方法內呼叫 @CommandHandler，造成 side effect
        return orderService.handle(new CreateOrderCommand(customerId));
    }
}
