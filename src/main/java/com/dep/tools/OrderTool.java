package com.dep.tools;

import com.dep.dtos.Order;
import com.dep.dtos.TopSellingProduct;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class OrderTool {

    private final List<Order> orders;

    public OrderTool(ObjectMapper objectMapper) {
        try (InputStream is = getClass().getResourceAsStream("/mock/orders.json")) {
            this.orders = objectMapper.readValue(is, Argument.listOf(Order.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load orders mock data", e);
        }
    }

    @Tool("Returns all orders placed in the last month")
    public List<Order> getOrdersLastMonth() {
        LocalDate start = LocalDate.now().minusMonths(1);
        return orders.stream()
                .filter(o -> LocalDate.parse(o.date()).isAfter(start))
                .collect(Collectors.toList());
    }

    @Tool("Returns all orders placed in the last year")
    public List<Order> getOrdersLastYear() {
        LocalDate start = LocalDate.now().minusYears(1);
        return orders.stream()
                .filter(o -> LocalDate.parse(o.date()).isAfter(start))
                .collect(Collectors.toList());
    }

    @Tool("Returns the single most expensive order ever placed")
    public Order getMostExpensiveOrder() {
        return orders.stream()
                .max(Comparator.comparing(Order::amount))
                .orElseThrow(() -> new RuntimeException("No orders found"));
    }

    @Tool("Returns all products ranked by total units sold from most to least, including total revenue per product")
    public List<TopSellingProduct> getTopSellingProducts() {
        return orders.stream()
                .collect(Collectors.groupingBy(Order::productName))
                .entrySet().stream()
                .map(e -> new TopSellingProduct(
                        e.getKey(),
                        e.getValue().stream().mapToInt(Order::quantity).sum(),
                        e.getValue().stream().map(Order::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .sorted(Comparator.comparingInt(TopSellingProduct::totalUnitsSold).reversed())
                .collect(Collectors.toList());
    }
}
