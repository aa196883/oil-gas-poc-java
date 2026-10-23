package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DeltaServiceTest {

    private final DeltaService service = new DeltaService();

    @Test
    void shouldComputeDeltasFromActualPrices() {
        LocalDate start = LocalDate.of(2026, 10, 9);
        List<PricePoint> prices = List.of(
            new PricePoint(start, new BigDecimal("110")),
            new PricePoint(start.plusDays(1), new BigDecimal("100")),
            new PricePoint(start.plusDays(2), new BigDecimal("90"))
        );

        List<DeltaPoint> result = service.getDeltas(prices, new BigDecimal("100"));

        assertEquals(List.of(
            new DeltaPoint(start, new BigDecimal("10.0000")),
            new DeltaPoint(start.plusDays(1), new BigDecimal("0.0000")),
            new DeltaPoint(start.plusDays(2), new BigDecimal("-10.0000"))
        ), result);
    }

    @Test
    void shouldRejectInvalidReferencePrice() {
        List<PricePoint> prices = List.of(
            new PricePoint(LocalDate.of(2026, 10, 9), new BigDecimal("100"))
        );

        assertThrows(IllegalArgumentException.class, () -> service.getDeltas(prices, BigDecimal.ZERO));
    }
}
