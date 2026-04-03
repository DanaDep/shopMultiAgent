package com.dep.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

@Serdeable
public record Refund(
        String id,
        String orderId,
        String customerId,
        String productName,
        BigDecimal amount,
        String date,
        String reason
) {}
