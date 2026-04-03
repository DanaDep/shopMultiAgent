package com.dep.tools;

import com.dep.dtos.Refund;
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
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class RefundTool {

    private final List<Refund> refunds;

    public RefundTool(ObjectMapper objectMapper) {
        try (InputStream is = getClass().getResourceAsStream("/mock/refunds.json")) {
            this.refunds = objectMapper.readValue(is, Argument.listOf(Refund.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load refunds mock data", e);
        }
    }

    @Tool("Returns all refunds issued in the last month")
    public List<Refund> getRefundsLastMonth() {
        LocalDate start = LocalDate.now().minusMonths(1);
        return refunds.stream()
                .filter(r -> LocalDate.parse(r.date()).isAfter(start))
                .collect(Collectors.toList());
    }

    @Tool("Returns the total monetary amount refunded in the last year")
    public BigDecimal getTotalRefundAmountLastYear() {
        LocalDate start = LocalDate.now().minusYears(1);
        return refunds.stream()
                .filter(r -> LocalDate.parse(r.date()).isAfter(start))
                .map(Refund::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Tool("Returns the name of the product that has been refunded the most times")
    public String getMostRefundedProduct() {
        return refunds.stream()
                .collect(Collectors.groupingBy(Refund::productName, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparing( Map.Entry::getValue ))
                .map( Map.Entry::getKey )
                .orElseThrow(() -> new RuntimeException("No refunds found"));
    }
}
