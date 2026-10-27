package com.semsoft.poc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SeriesServiceTest {

    @Test
    void shouldRejectInvalidDates() {

        SeriesRepository repository = mock(SeriesRepository.class);
        SeriesService service = new SeriesService(repository, new UnitConverter());

        LocalDate start = LocalDate.of(2026, 12, 31);
        LocalDate end = LocalDate.of(2026, 1, 1);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.getPrices(1, start, end, "USD/BBL")
        );
    }

    @Test
    void shouldReturnPrices() {

        SeriesRepository repository = mock(SeriesRepository.class);
        SeriesService service = new SeriesService(repository, new UnitConverter());

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        List<PricePoint> expected = List.of(
            new PricePoint(
                LocalDate.of(2026, 1, 5),
                new java.math.BigDecimal("75.50")
            )
        );

        when(repository.existsById(1L)).thenReturn(true);
        when(repository.findBenchmarkUnit(1L)).thenReturn(Optional.of("USD/BBL"));
        when(repository.findBenchmarkFamily(1L)).thenReturn(Optional.of("Crude Oil (Brut)"));
        when(repository.findPrices(1L, start, end)).thenReturn(expected);

        List<PricePoint> result = service.getPrices(1L, start, end, "USD/BBL");

        assertEquals(expected.getFirst().date(), result.getFirst().date());
        assertEquals(0, expected.getFirst().price().compareTo(result.getFirst().price()));

        verify(repository).findPrices(1L, start, end);
    }

    @Test
    void shouldReturnPricesInRequestedUnit() {
        SeriesRepository repository = mock(SeriesRepository.class);
        SeriesService service = new SeriesService(repository, new UnitConverter());
        LocalDate date = LocalDate.of(2026, 1, 5);

        when(repository.existsById(1L)).thenReturn(true);
        when(repository.findBenchmarkUnit(1L)).thenReturn(Optional.of("USD/BBL"));
        when(repository.findBenchmarkFamily(1L)).thenReturn(Optional.of("Crude Oil (Brut)"));
        when(repository.findPrices(1L, date, date)).thenReturn(
            List.of(new PricePoint(date, new BigDecimal("84")))
        );

        List<PricePoint> result = service.getPrices(1L, date, date, "USD/Gallon");

        assertEquals(1, result.size());
        assertEquals(date, result.getFirst().date());
        assertEquals(0, new BigDecimal("2").compareTo(result.getFirst().price()));
    }
}
