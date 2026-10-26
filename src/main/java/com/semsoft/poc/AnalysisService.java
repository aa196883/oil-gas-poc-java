package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    private final SeriesService seriesService;

    public AnalysisService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    Map<Double, BigDecimal> getQuantiles(
        long seriesId,
        LocalDate start,
        LocalDate end,
        String unit,
        Long freightId
    ) {
        List<PricePoint> prices = freightId == null
            ? seriesService.getPrices(seriesId, start, end, unit)
            : seriesService.getPricesWithFreight(seriesId, freightId, start, end, unit);

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
            double position = (sorted.size() - 1) * q;
            int lower = (int) Math.floor(position);
            int upper = (int) Math.ceil(position);

            BigDecimal weight = BigDecimal.valueOf(position - lower);

            BigDecimal value = sorted.get(lower)
                .add(sorted.get(upper).subtract(sorted.get(lower)).multiply(weight));

            result.put(q, value);
        }

        return result;
    }
}
