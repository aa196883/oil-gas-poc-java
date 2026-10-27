package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

@Service
public class PriceService {

    // All supplied prices must already be expressed in the same unit.
    public List<PricePoint> transform(TransformRequest request) {
        if (request == null || request.operations() == null) {
            throw new IllegalArgumentException("Operations are required");
        }
        validatePrices(request.prices());
        List<PricePoint> result = List.copyOf(request.prices());
        for (Operation operation : request.operations()) {
            if (operation == null || operation.type() == null) {
                throw new IllegalArgumentException("Operation type is required");
            }
            switch (operation.type()) {
                case "ADD_FREIGHT" -> {
                    validatePrices(operation.prices());
                    NavigableMap<LocalDate, BigDecimal> freight = new TreeMap<>();
                    operation.prices().forEach(point -> freight.put(point.date(), point.price()));
                    result = result.stream().map(point -> {
                        var match = freight.floorEntry(point.date());
                        if (match == null) {
                            throw new IllegalArgumentException(
                                "No freight price available on or before " + point.date()
                            );
                        }
                        return new PricePoint(point.date(), point.price().add(match.getValue()));
                    }).toList();
                }
                case "MULTIPLY", "ADD" -> {
                    if (operation.value() == null) {
                        throw new IllegalArgumentException(operation.type() + " requires a value");
                    }
                    result = result.stream().map(point -> new PricePoint(
                        point.date(),
                        "MULTIPLY".equals(operation.type())
                            ? point.price().multiply(operation.value())
                            : point.price().add(operation.value())
                    )).toList();
                }
                default -> throw new IllegalArgumentException("Unknown operation: " + operation.type());
            }
        }
        return result;
    }

    static void validatePrices(List<PricePoint> prices) {
        if (prices == null || prices.stream().anyMatch(point ->
            point == null || point.date() == null || point.price() == null
        )) {
            throw new IllegalArgumentException("Prices must contain a date and price for every point");
        }
    }
}
