package com.dep.dtos;

public record ProductRating(
        String productName,
        double averageRating,
        int reviewCount
) {}
