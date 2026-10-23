package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DeltaServiceTest {

    @Test
    void shouldConvertBothNativeAndReferencePricesBeforeComputingDeltas() {
        SeriesRepository repository = mock(SeriesRepository.class);
        DeltaService service = new DeltaService(repository, new UnitConverter());
        LocalDate start = LocalDate.of(2026, 10, 9);
        LocalDate end = start.plusDays(2);

        when(repository.findBenchmarkUnit(1L)).thenReturn(Optional.of("USD/BBL"));
        when(repository.findBenchmarkFamily(1L)).thenReturn(Optional.of("Crude Oil (Brut)"));
        when(repository.findPrices(1L, start, end)).thenReturn(List.of(
            new PricePoint(start, new BigDecimal("91.85")),
            new PricePoint(start.plusDays(1), new BigDecimal("84")),
            new PricePoint(end, new BigDecimal("75.60"))
        ));

        // 2 USD/gallon = 84 USD/barrel = 615.72 USD/MT.
        // Both native and reference units have distinct, non-identity conversion factors.
        List<DeltaPoint> result = service.getDeltas(
            1L, start, end, new BigDecimal("2"), "USD/Gallon"
        );

        assertEquals(List.of(
            new DeltaPoint(start, new BigDecimal("9.3452")),
            new DeltaPoint(start.plusDays(1), new BigDecimal("0.0000")),
            new DeltaPoint(end, new BigDecimal("-10.0000"))
        ), result);
        verify(repository).findBenchmarkUnit(1L);
    }

    @Test
    void shouldComputeWtiDeltaAgainstReferenceInBarrels() {
        SeriesRepository repository = mock(SeriesRepository.class);
        DeltaService service = new DeltaService(repository, new UnitConverter());
        LocalDate date = LocalDate.of(2026, 10, 9);

        when(repository.findBenchmarkUnit(1L)).thenReturn(Optional.of("USD/BBL"));
        when(repository.findBenchmarkFamily(1L)).thenReturn(Optional.of("Crude Oil (Brut)"));
        when(repository.findPrices(1L, date, date)).thenReturn(
            List.of(new PricePoint(date, new BigDecimal("91.85")))
        );

        List<DeltaPoint> result = service.getDeltas(
            1L, date, date, new BigDecimal("90"), "USD/BBL"
        );

        assertEquals(List.of(new DeltaPoint(date, new BigDecimal("2.0556"))), result);
    }

    @Test
    void shouldComputeDeltas() {

        SeriesRepository repository = mock(SeriesRepository.class);
        UnitConverter converter = new UnitConverter();
        DeltaService service = new DeltaService(repository, converter);

        long seriesId = 1L;
        LocalDate date = LocalDate.of(2026, 1, 5);

        when(repository.findBenchmarkUnit(seriesId)).thenReturn(
            Optional.of("USD/BBL")
        );

        when(repository.findBenchmarkFamily(seriesId)).thenReturn(
            Optional.of("Crude Oil (Brut)")
        );

        when(repository.findPrices(seriesId, date, date)).thenReturn(
            List.of(new PricePoint(date, new BigDecimal("75.50")))
        );

        List<DeltaPoint> result = service.getDeltas(
            seriesId,
            date,
            date,
            new BigDecimal("550"),
            "USD/MT"
        );

        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("0.6209").compareTo(result.get(0).delta()));
    }
}
