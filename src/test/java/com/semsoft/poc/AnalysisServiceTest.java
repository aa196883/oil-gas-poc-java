package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisServiceTest {

    @Test
    void shouldGetMeanWithOptionalFreightIncludingEarlierFreight() {
        SeriesService series = mock(SeriesService.class);
        AnalysisService service = new AnalysisService(series, new PriceService());
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = start.plusDays(2);
        when(series.getPrices(1L, start, end, "USD/MT")).thenReturn(List.of(
            new PricePoint(start, new BigDecimal("10")),
            new PricePoint(end, new BigDecimal("20"))
        ));
        when(series.getPrices(2L, LocalDate.of(1, 1, 1), end, "USD/MT")).thenReturn(List.of(
            new PricePoint(start.minusDays(1), new BigDecimal("2")),
            new PricePoint(end, new BigDecimal("4"))
        ));

        assertEquals(0, new BigDecimal("15").compareTo(service.getMean(1L, start, end, "USD/MT", null)));
        assertEquals(0, new BigDecimal("18").compareTo(service.getMean(1L, start, end, "USD/MT", 2L)));
    }

    @Test
    void shouldHandleRepeatingMeanAndRejectEmptyPrices() {
        AnalysisService service = new AnalysisService(mock(SeriesService.class), new PriceService());
        LocalDate date = LocalDate.of(2026, 1, 1);
        assertEquals(new BigDecimal("0.3333333333333333333333333333333333"), service.computeMean(List.of(
            new PricePoint(date, BigDecimal.ZERO),
            new PricePoint(date.plusDays(1), BigDecimal.ZERO),
            new PricePoint(date.plusDays(2), BigDecimal.ONE)
        )));
        assertThrows(IllegalArgumentException.class, () -> service.computeMean(List.of()));
    }

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
