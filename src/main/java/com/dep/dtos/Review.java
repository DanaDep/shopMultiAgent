package com.dep.dtos;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Review(
        String id,
        String customerId,
        String productName,
        int rating,
        String comment,
        String date
) {}
