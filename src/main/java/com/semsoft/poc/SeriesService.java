package com.semsoft.poc;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SeriesService {

    private final SeriesRepository repository;

    public SeriesService(SeriesRepository repository) {
        this.repository = repository;
    }

    public List<SeriesSummary> getAllSeries() {
        return repository.findAll();
    }

    public List<PricePoint> getPrices(
        long seriesId,
        LocalDate start,
        LocalDate end
    ) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                "Start date must be before or equal to end date"
            );
        }

        if (!repository.existsById(seriesId)) {
            throw new SeriesNotFoundException(seriesId);
        }

        return repository.findPrices(seriesId, start, end);
    }
}