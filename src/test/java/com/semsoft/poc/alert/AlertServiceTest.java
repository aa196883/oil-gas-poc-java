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
            assertEquals("The declared price is 20% " + (declared == 80 ? "below" : "above")
                + " the average market price of 100 (limit: 15%).", alert.message());
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
        assertEquals(List.of(new Alert("TIME_OUTSIDE_MARGINS", "high",
            "The declared price is outside the tolerated margins for 75% of observations (3 of 4; limit: 50%).")), alerts);
        assertEquals(List.of(), service.evaluate(prices, new BigDecimal("100"),
            points(101, 101, 90, 90), points(110, 110, 110, 110)));
        // Test the declared price, even when the market prices themselves fit the bounds.
        assertTrue(service.evaluate(points(90, 90), new BigDecimal("100"), points(85, 85), points(95, 95))
            .stream().anyMatch(a -> a.type().equals("TIME_OUTSIDE_MARGINS")));
    }

    @Test
    void shouldAlertOnLargePositiveOrNegativeZscoreWithoutDisplayingScore() {
        for (int declared : new int[] {90, 110}) {
            Alert alert = evaluate(points(99, 100, 101), declared).stream()
                .filter(a -> a.type().equals("ZSCORE")).findFirst().orElseThrow();
            assertEquals(new Alert("ZSCORE", "high",
                "The declared price is unusually far from the market median, given the observed price dispersion."), alert);
        }
        assertFalse(evaluate(points(99, 100, 101), 105).stream().anyMatch(a -> a.type().equals("ZSCORE")));
        assertFalse(evaluate(points(100, 100, 100), 110).stream().anyMatch(a -> a.type().equals("ZSCORE")));
    }

    @Test
    void shouldDistinguishUpperAndLowerQuantileAlertsAndExcludeBoundaries() {
        List<PricePoint> prices = points(90, 110);
        assertEquals(List.of(new Alert("ABOVE_Q95", "medium",
            "The declared price is above the 95th percentile of market prices (109).")), evaluate(prices, 110));
        assertEquals(List.of(new Alert("BELOW_Q05", "medium",
            "The declared price is below the 5th percentile of market prices (91).")), evaluate(prices, 90));
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
