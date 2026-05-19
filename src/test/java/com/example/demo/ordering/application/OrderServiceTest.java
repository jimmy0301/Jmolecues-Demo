package com.example.demo.ordering.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.ordering.application.command.CancelOrderCommand;
import com.example.demo.ordering.application.command.CreateOrderCommand;
import com.example.demo.ordering.application.command.PlaceOrderCommand;
import com.example.demo.ordering.domain.Order;
import com.example.demo.ordering.domain.OrderCancelled;
import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderPlaced;
import com.example.demo.ordering.domain.OrderRepository;
import com.example.demo.ordering.domain.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;

    @InjectMocks private OrderService orderService;

    @Test
    void handle_createOrderCommand_savesNewOrder() {
        var customerId = UUID.randomUUID();
        var command = new CreateOrderCommand(customerId);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orderService.handle(command);

        verify(orderRepository).save(any(Order.class));
        assertThat(result.getCustomer().id()).isEqualTo(customerId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void handle_placeOrderCommand_registersEventAndSaves() {
        var order = new Order(UUID.randomUUID());
        var command = new PlaceOrderCommand(order.getId());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orderService.handle(command);

        var orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        var savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getRegisteredEvents()).hasSize(1);
        var event = (OrderPlaced) savedOrder.getRegisteredEvents().iterator().next();
        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void handle_cancelOrderCommand_registersEventAndSaves() {
        var order = new Order(UUID.randomUUID());
        var command = new CancelOrderCommand(order.getId());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orderService.handle(command);

        var orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        var savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getRegisteredEvents()).hasSize(1);
        var event = (OrderCancelled) savedOrder.getRegisteredEvents().iterator().next();
        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void handle_placeOrderCommand_orderNotFound_throws() {
        var orderId = OrderId.create();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.handle(new PlaceOrderCommand(orderId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void handle_cancelOrderCommand_orderNotFound_throws() {
        var orderId = OrderId.create();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.handle(new CancelOrderCommand(orderId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }
}
