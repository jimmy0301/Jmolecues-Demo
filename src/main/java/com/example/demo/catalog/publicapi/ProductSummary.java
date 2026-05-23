package com.example.demo.catalog.publicapi;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummary(UUID id, String name, BigDecimal amount, String currency) {}
