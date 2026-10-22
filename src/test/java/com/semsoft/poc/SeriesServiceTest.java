package com.semsoft.poc;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SeriesServiceTest {

    @Test
    void shouldRejectInvalidDates() {

        SeriesRepository repository = mock(SeriesRepository.class);
        SeriesService service = new SeriesService(repository);

        LocalDate start = LocalDate.of(2026, 12, 31);
        LocalDate end = LocalDate.of(2026, 1, 1);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.getPrices(1, start, end)
        );
    }

    @Test
    void shouldReturnPrices() {

        SeriesRepository repository = mock(SeriesRepository.class);
        SeriesService service = new SeriesService(repository);

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        List<PricePoint> expected = List.of(
            new PricePoint(
                LocalDate.of(2026, 1, 5),
                new java.math.BigDecimal("75.50")
            )
        );

        when(repository.existsById(1L)).thenReturn(true);
        when(repository.findPrices(1L, start, end)).thenReturn(expected);

        List<PricePoint> result = service.getPrices(1L, start, end);

        assertEquals(expected, result);

        verify(repository).findPrices(1L, start, end);
    }
}