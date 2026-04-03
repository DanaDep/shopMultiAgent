package com.dep.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

@Serdeable
public record Order(
        String id,
        String customerId,
        String productName,
        String category,
        BigDecimal amount,
        int quantity,
        String date,
        String status
) {}
