package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AnalysisServiceTest {

    @Test
    void shouldComputeQuantilesWithLinearInterpolation() {
        AnalysisService service = new AnalysisService(mock(SeriesService.class), new PriceService());
        LocalDate date = LocalDate.of(2026, 1, 1);
        List<PricePoint> prices = List.of(
            new PricePoint(date, new BigDecimal("50")),
            new PricePoint(date, new BigDecimal("10")),
            new PricePoint(date, new BigDecimal("40")),
            new PricePoint(date, new BigDecimal("20")),
            new PricePoint(date, new BigDecimal("30"))
        );

        Map<Double, BigDecimal> result = service.computeQuantiles(prices);

        assertEquals(List.of(0.05, 0.25, 0.50, 0.75, 0.95), List.copyOf(result.keySet()));
        assertEquals(0, new BigDecimal("12.0").compareTo(result.get(0.05)));
        assertEquals(0, new BigDecimal("20.0").compareTo(result.get(0.25)));
        assertEquals(0, new BigDecimal("30.0").compareTo(result.get(0.50)));
        assertEquals(0, new BigDecimal("40.0").compareTo(result.get(0.75)));
        assertEquals(0, new BigDecimal("48.0").compareTo(result.get(0.95)));
    }
}
