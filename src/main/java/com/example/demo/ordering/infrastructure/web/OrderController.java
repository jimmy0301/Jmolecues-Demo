package com.example.demo.ordering.infrastructure.web;

import com.example.demo.ordering.api.OrdersApi;
import com.example.demo.ordering.api.model.CreateOrderRequest;
import com.example.demo.ordering.api.model.OrderItemResponse;
import com.example.demo.ordering.api.model.OrderResponse;
import com.example.demo.ordering.api.model.OrderResponse.StatusEnum;
import com.example.demo.ordering.application.OrderQueryModel;
import com.example.demo.ordering.application.OrderService;
import com.example.demo.ordering.application.command.CancelOrderCommand;
import com.example.demo.ordering.application.command.CreateOrderCommand;
import com.example.demo.ordering.application.command.PlaceOrderCommand;
import com.example.demo.ordering.domain.Order;
import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderItem;
import com.example.demo.shared.CustomerId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OrderController implements OrdersApi {

    private final OrderQueryModel orderQueryModel;
    private final OrderService orderService;

    OrderController(OrderQueryModel orderQueryModel, OrderService orderService) {
        this.orderQueryModel = orderQueryModel;
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<OrderResponse> getOrderById(UUID id) {
        return orderQueryModel
                .findById(new OrderId(id))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(CreateOrderRequest createOrderRequest) {
        var command = new CreateOrderCommand(new CustomerId(createOrderRequest.getCustomerId()));
        var order = orderService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    @Override
    public ResponseEntity<OrderResponse> placeOrder(UUID id) {
        var order = orderService.handle(new PlaceOrderCommand(new OrderId(id)));
        return ResponseEntity.ok(toResponse(order));
    }

    @Override
    public ResponseEntity<Void> cancelOrder(UUID id) {
        orderService.handle(new CancelOrderCommand(new OrderId(id)));
        return ResponseEntity.noContent().build();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items =
                order.getItems().stream().map(this::toItemResponse).toList();
        return new OrderResponse(
                order.getId().id(),
                order.getCustomerId().id(),
                StatusEnum.fromValue(order.getStatus().name()),
                items);
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        var unitPrice =
                new com.example.demo.ordering.api.model.Money(
                        item.getUnitPrice().amount(), item.getUnitPrice().currency());
        return new OrderItemResponse(
                item.getId().id(), item.getProductId().id(), item.getQuantity().value(), unitPrice);
    }
}
