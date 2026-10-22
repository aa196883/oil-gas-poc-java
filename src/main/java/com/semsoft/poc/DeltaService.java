package com.semsoft.poc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class DeltaService {

    private final SeriesRepository repository;
    private final UnitConverter converter;

    public DeltaService(SeriesRepository repository, UnitConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }

    public List<DeltaPoint> getDeltas(
        long seriesId,
        LocalDate start,
        LocalDate end,
        BigDecimal referencePrice
    ) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Invalid date range");
        }
        
        if (referencePrice == null || referencePrice.signum() <= 0) {
            throw new IllegalArgumentException("Reference price must be strictly positive");
        }

        String benchmarkId = repository.findBenchmarkId(seriesId)
            .orElseThrow(() -> new SeriesNotFoundException(seriesId));

        List<PricePoint> prices = repository.findPrices(seriesId, start, end);

        BigDecimal factor = converter.getFactor(benchmarkId);

        return prices.stream()
            .map(price -> new DeltaPoint(
                price.date(),
                price.price()
                    .multiply(factor)
                    .subtract(referencePrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(referencePrice, 4, RoundingMode.HALF_UP)
            ))
            .toList();
    }
}