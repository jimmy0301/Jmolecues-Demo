package com.example.demo.ordering.application;

import com.example.demo.ordering.domain.OrderId;
import com.example.demo.ordering.domain.OrderStatus;

public record OrderSummary(OrderId orderId, OrderStatus status) {}
