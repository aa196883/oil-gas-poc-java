package com.semsoft.poc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SeriesService {

    private final SeriesRepository repository;
    private final UnitConverter converter;

    public SeriesService(SeriesRepository repository, UnitConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }

    public List<SeriesSummary> getAllSeries() {
        return repository.findAll();
    }

    public List<PricePoint> getPrices(
        long seriesId,
        LocalDate start,
        LocalDate end,
        String unit
    ) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                "Start date must be before or equal to end date"
            );
        }

        if (!repository.existsById(seriesId)) {
            throw new SeriesNotFoundException(seriesId);
        }

        String nativeUnit = repository.findBenchmarkUnit(seriesId)
            .orElseThrow(() -> new SeriesNotFoundException(seriesId));
        String family = repository.findBenchmarkFamily(seriesId)
            .orElseThrow(() -> new SeriesNotFoundException(seriesId));
        BigDecimal nativeFactor = repository.isFreight(seriesId)
            ? converter.getFreightFactor(nativeUnit, family, repository.findBenchmarkZone(seriesId).orElse(null))
            : converter.getCommodityFactor(nativeUnit, family);
        BigDecimal requestedFactor = converter.getCommodityFactor(unit, family);

        return repository.findPrices(seriesId, start, end).stream()
            .map(point -> new PricePoint(
                point.date(),
                point.price().multiply(nativeFactor).divide(requestedFactor, MathContext.DECIMAL64)
            ))
            .toList();
    }

}
