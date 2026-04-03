package com.dep.dtos;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Return(
        String id,
        String orderId,
        String customerId,
        String productName,
        String date,
        String reason
) {}
