package com.dep.tools;

import com.dep.dtos.Return;
import com.dep.dtos.ReturnByReason;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ReturnTool {

    private final List<Return> returns;

    public ReturnTool(ObjectMapper objectMapper) {
        try (InputStream is = getClass().getResourceAsStream("/mock/returns.json")) {
            this.returns = objectMapper.readValue(is, Argument.listOf(Return.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load returns mock data", e);
        }
    }

    @Tool("Returns all product returns submitted in the last month")
    public List<Return> getReturnsLastMonth() {
        LocalDate start = LocalDate.now().minusMonths(1);
        return returns.stream()
                .filter(r -> LocalDate.parse(r.date()).isAfter(start))
                .collect(Collectors.toList());
    }

    @Tool("Returns the count of product returns grouped by return reason")
    public List<ReturnByReason> getReturnsByReason() {
        return returns.stream()
                .collect(Collectors.groupingBy(Return::reason, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ReturnByReason(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparingInt(ReturnByReason::count).reversed())
                .collect(Collectors.toList());
    }

    @Tool("Returns the name of the product that has been returned the most times")
    public String getMostReturnedProduct() {
        return returns.stream()
                .collect(Collectors.groupingBy(Return::productName, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparing(e -> e.getValue()))
                .map(e -> e.getKey())
                .orElseThrow(() -> new RuntimeException("No returns found"));
    }
}
