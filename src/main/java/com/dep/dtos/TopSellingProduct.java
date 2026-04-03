package com.dep.dtos;

import java.math.BigDecimal;

public record TopSellingProduct(
        String productName,
        int totalUnitsSold,
        BigDecimal totalRevenue
) {}
