package com.semsoft.poc.alert;

import com.semsoft.poc.AnalysisService;
import com.semsoft.poc.PricePoint;
import com.semsoft.poc.PriceService;
import com.semsoft.poc.SeriesService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AlertServiceTest {

    private final AlertService service = new AlertService(
        new AnalysisService(mock(SeriesService.class), new PriceService()));

    @Test
    void shouldAlertOnMeanGapInEitherDirectionOnlyBeyondThreshold() {
        List<PricePoint> prices = points(90, 100, 110);
        for (int declared : new int[] {80, 120}) {
            Alert alert = evaluate(prices, declared).stream()
                .filter(a -> a.type().equals("MEAN_GAP")).findFirst().orElseThrow();
            assertEquals("high", alert.severity());
        }
        for (int declared : new int[] {85, 115}) {
            assertFalse(evaluate(prices, declared).stream().anyMatch(a -> a.type().equals("MEAN_GAP")));
        }
    }

    @Test
    void shouldAlertWhenDeclaredPriceIsOutsideMarginsForMoreThanHalfOfObservations() {
        List<PricePoint> prices = points(100, 100, 100, 100);
        List<Alert> alerts = service.evaluate(prices, new BigDecimal("100"),
            points(101, 101, 101, 90), points(110, 110, 110, 110));
        assertEquals(1, alerts.size());
        assertEquals("TIME_OUTSIDE_MARGINS", alerts.getFirst().type());
        assertEquals("high", alerts.getFirst().severity());
        assertEquals(List.of(), service.evaluate(prices, new BigDecimal("100"),
            points(101, 101, 90, 90), points(110, 110, 110, 110)));
        // Test the declared price, even when the market prices themselves fit the bounds.
        assertTrue(service.evaluate(points(90, 90), new BigDecimal("100"), points(85, 85), points(95, 95))
            .stream().anyMatch(a -> a.type().equals("TIME_OUTSIDE_MARGINS")));
    }

    @Test
    void shouldAlertOnLargePositiveOrNegativeZscore() {
        for (int declared : new int[] {90, 110}) {
            Alert alert = evaluate(points(99, 100, 101), declared).stream()
                .filter(a -> a.type().equals("ZSCORE")).findFirst().orElseThrow();
            assertEquals("high", alert.severity());
        }
        assertFalse(evaluate(points(99, 100, 101), 105).stream().anyMatch(a -> a.type().equals("ZSCORE")));
        assertFalse(evaluate(points(100, 100, 100), 110).stream().anyMatch(a -> a.type().equals("ZSCORE")));
    }

    @Test
    void shouldDistinguishUpperAndLowerQuantileAlertsAndExcludeBoundaries() {
        List<PricePoint> prices = points(90, 110);
        List<Alert> upperAlerts = evaluate(prices, 110);
        assertEquals(1, upperAlerts.size());
        assertEquals("ABOVE_Q95", upperAlerts.getFirst().type());
        assertEquals("medium", upperAlerts.getFirst().severity());
        List<Alert> lowerAlerts = evaluate(prices, 90);
        assertEquals(1, lowerAlerts.size());
        assertEquals("BELOW_Q05", lowerAlerts.getFirst().type());
        assertEquals("medium", lowerAlerts.getFirst().severity());
        assertEquals(List.of(), evaluate(prices, 91));
        assertEquals(List.of(), evaluate(prices, 109));
    }

    @Test
    void shouldSkipUndefinedMeanGap() {
        assertFalse(evaluate(points(0, 0), 100).stream().anyMatch(a -> a.type().equals("MEAN_GAP")));
    }

    private List<Alert> evaluate(List<PricePoint> prices, int declared) {
        return service.evaluate(prices, BigDecimal.valueOf(declared),
            prices.stream().map(p -> new PricePoint(p.date(), BigDecimal.ZERO)).toList(),
            prices.stream().map(p -> new PricePoint(p.date(), new BigDecimal("1000"))).toList());
    }

    private static List<PricePoint> points(int... values) {
        return IntStream.range(0, values.length)
            .mapToObj(i -> new PricePoint(LocalDate.of(2026, 1, 1).plusDays(i), BigDecimal.valueOf(values[i])))
            .toList();
    }
}
