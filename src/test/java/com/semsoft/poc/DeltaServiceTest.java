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
    void shouldComputeDeltas() {

        SeriesRepository repository = mock(SeriesRepository.class);
        UnitConverter converter = new UnitConverter();
        DeltaService service = new DeltaService(repository, converter);

        long seriesId = 1L;
        LocalDate date = LocalDate.of(2026, 1, 5);

        when(repository.findBenchmarkId(seriesId)).thenReturn(
            Optional.of("wti")
        );

        when(repository.findPrices(seriesId, date, date)).thenReturn(
            List.of(new PricePoint(date, new BigDecimal("75.50")))
        );

        List<DeltaPoint> result = service.getDeltas(
            seriesId,
            date,
            date,
            new BigDecimal("550")
        );

        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("0.6209").compareTo(result.get(0).delta()));
    }
}