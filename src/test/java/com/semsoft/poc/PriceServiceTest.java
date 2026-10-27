package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceServiceTest {
    private final PriceService service = new PriceService();

    private PricePoint point(String date, String price) {
        return new PricePoint(LocalDate.parse(date), new BigDecimal(price));
    }

    @Test
    void shouldApplyOperationsInOrderWithoutChangingInput() {
        List<PricePoint> prices = List.of(point("2026-10-01", "500"), point("2026-10-02", "510"));
        List<PricePoint> result = service.transform(new TransformRequest(prices, List.of(
            new Operation("ADD_FREIGHT", null, List.of(point("2026-10-01", "30"), point("2026-10-02", "32"))),
            new Operation("MULTIPLY", new BigDecimal("1.10"), null),
            new Operation("ADD", new BigDecimal("20"), null)
        )));
        assertEquals(List.of(point("2026-10-01", "603.00"), point("2026-10-02", "616.20")), result);
        assertEquals(new BigDecimal("500"), prices.getFirst().price());
        assertEquals(prices, service.transform(new TransformRequest(prices, List.of())));
    }

    @Test
    void shouldMatchLastKnownFreightByDateRegardlessOfInputOrder() {
        List<PricePoint> result = service.transform(new TransformRequest(
            List.of(point("2026-10-03", "100"), point("2026-10-01", "100")),
            List.of(new Operation("ADD_FREIGHT", null, List.of(
                point("2026-10-04", "999"), point("2026-10-02", "32"), point("2026-09-30", "30")
            )))
        ));
        assertEquals(List.of(point("2026-10-03", "132"), point("2026-10-01", "130")), result);
    }

    @Test
    void shouldRejectMissingFreightHistoryAndInvalidOperations() {
        var prices = List.of(point("2026-10-01", "500"));
        for (Operation operation : List.of(
            new Operation("ADD_FREIGHT", null, List.of(point("2026-10-02", "30"))),
            new Operation("MULTIPLY", null, null),
            new Operation("ADD", null, null),
            new Operation("ADD_FREIGHT", null, null),
            new Operation("UNKNOWN", BigDecimal.ONE, null)
        )) {
            assertThrows(IllegalArgumentException.class, () ->
                service.transform(new TransformRequest(prices, List.of(operation))));
        }
    }

    @Test
    void shouldSupportNegativeAndZeroValuesAndEmptyPrices() {
        var prices = List.of(point("2026-10-01", "100"));
        assertEquals(List.of(point("2026-10-01", "-20")), service.transform(new TransformRequest(
            prices, List.of(new Operation("MULTIPLY", BigDecimal.ZERO, null),
                new Operation("ADD", new BigDecimal("-20"), null))
        )));
        assertEquals(List.of(), service.transform(new TransformRequest(
            List.of(), List.of(new Operation("MULTIPLY", BigDecimal.ONE, null))
        )));
    }
}
