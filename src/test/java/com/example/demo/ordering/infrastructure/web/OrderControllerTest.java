package com.example.demo.ordering.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.ordering.application.OrderQueryModel;
import com.example.demo.ordering.application.OrderService;
import com.example.demo.ordering.application.command.CancelOrderCommand;
import com.example.demo.ordering.application.command.CreateOrderCommand;
import com.example.demo.ordering.application.command.PlaceOrderCommand;
import com.example.demo.ordering.domain.Order;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService;
    @MockBean OrderQueryModel orderQueryModel;

    @Test
    void createOrder_returns201WithPendingStatus() throws Exception {
        var customerId = UUID.randomUUID();
        var order = new Order(customerId);
        when(orderService.handle(any(CreateOrderCommand.class))).thenReturn(order);

        mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"customerId\":\"" + customerId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getOrderById_whenFound_returns200() throws Exception {
        var order = new Order(UUID.randomUUID());
        when(orderQueryModel.findById(any())).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/{id}", order.getId().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getOrderById_whenNotFound_returns404() throws Exception {
        when(orderQueryModel.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void placeOrder_returns200WithPlacedStatus() throws Exception {
        var order = new Order(UUID.randomUUID());
        order.place();
        when(orderService.handle(any(PlaceOrderCommand.class))).thenReturn(order);

        mockMvc.perform(post("/orders/{id}/place", order.getId().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    void cancelOrder_returns204() throws Exception {
        var order = new Order(UUID.randomUUID());
        when(orderService.handle(any(CancelOrderCommand.class))).thenReturn(order);

        mockMvc.perform(post("/orders/{id}/cancel", order.getId().id()))
                .andExpect(status().isNoContent());
    }
}
