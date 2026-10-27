package com.semsoft.poc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    private final SeriesService seriesService;
    private final PriceService priceService;

    public AnalysisService(SeriesService seriesService, PriceService priceService) {
        this.seriesService = seriesService;
        this.priceService = priceService;
    }

    Map<Double, BigDecimal> getQuantiles(
        long seriesId,
        LocalDate start,
        LocalDate end,
        String unit,
        Long freightId
    ) {
        List<PricePoint> prices = seriesService.getPrices(seriesId, start, end, unit);
        if (freightId != null) {
            List<PricePoint> freight = seriesService.getPrices(
                freightId, LocalDate.of(1, 1, 1), end, unit
            );
            prices = priceService.transform(new TransformRequest(
                prices, List.of(new Operation("ADD_FREIGHT", null, freight))
            ));
        }

        return computeQuantiles(prices);
    }

    public Map<Double, BigDecimal> computeQuantiles(List<PricePoint> prices) {
        if (prices.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute quantiles on empty prices");
        }

        List<BigDecimal> sorted = prices.stream()
            .map(PricePoint::price)
            .sorted()
            .toList();

        Map<Double, BigDecimal> result = new LinkedHashMap<>();

        for (double q : List.of(0.05, 0.25, 0.50, 0.75, 0.95)) {
            BigDecimal position = BigDecimal.valueOf(q)
                .multiply(BigDecimal.valueOf(sorted.size() - 1L));
            int lower = position.setScale(0, RoundingMode.FLOOR).intValueExact();
            int upper = position.setScale(0, RoundingMode.CEILING).intValueExact();

            BigDecimal weight = position.subtract(BigDecimal.valueOf(lower));

            BigDecimal value = sorted.get(lower)
                .add(sorted.get(upper).subtract(sorted.get(lower)).multiply(weight));

            result.put(q, value);
        }

        return result;
    }
}
