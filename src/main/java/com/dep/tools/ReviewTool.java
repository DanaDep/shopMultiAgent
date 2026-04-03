package com.dep.tools;

import com.dep.dtos.ProductRating;
import com.dep.dtos.Review;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ReviewTool {

    private final List<Review> reviews;

    public ReviewTool(ObjectMapper objectMapper) {
        try (InputStream is = getClass().getResourceAsStream("/mock/reviews.json")) {
            this.reviews = objectMapper.readValue(is, Argument.listOf(Review.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load reviews mock data", e);
        }
    }

    @Tool("Returns the average star rating for each product, along with the number of reviews")
    public List<ProductRating> getAverageRatingPerProduct() {
        return reviews.stream()
                .collect(Collectors.groupingBy(Review::productName))
                .entrySet().stream()
                .map(e -> new ProductRating(
                        e.getKey(),
                        e.getValue().stream().mapToInt(Review::rating).average().orElse(0.0),
                        e.getValue().size()
                ))
                .sorted(Comparator.comparingDouble(ProductRating::averageRating).reversed())
                .collect(Collectors.toList());
    }

    @Tool("Returns the top 3 best-rated products by average customer rating")
    public List<ProductRating> getBestRatedProducts() {
        return getAverageRatingPerProduct().stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    @Tool("Returns the 3 worst-rated products by average customer rating")
    public List<ProductRating> getWorstRatedProducts() {
        return getAverageRatingPerProduct().stream()
                .sorted(Comparator.comparingDouble(ProductRating::averageRating))
                .limit(3)
                .collect(Collectors.toList());
    }

    @Tool("Returns all recent customer reviews with a rating of 2 stars or below")
    public List<Review> getRecentNegativeReviews() {
        return reviews.stream()
                .filter(r -> r.rating() <= 2)
                .sorted(Comparator.comparing(Review::date).reversed())
                .collect(Collectors.toList());
    }
}
