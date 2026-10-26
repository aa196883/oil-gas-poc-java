package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeriesControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SeriesRepository repository = mock(SeriesRepository.class);
        UnitConverter converter = new UnitConverter();
        mvc = MockMvcBuilders.standaloneSetup(new SeriesController(
            new SeriesService(repository, converter),
            new DeltaService()
        )).setControllerAdvice(new GlobalExceptionHandler()).build();

        when(repository.findAll()).thenReturn(List.of(
            new SeriesSummary(1L, "wti", null, "test", null, "USD", "USD/BBL", "Crude Oil", false, "POL"),
            new SeriesSummary(2L, "freight", null, "test", null, "USD", "USD/MT", "Freight", true, "POD")
        ));

        LocalDate date = LocalDate.of(2026, 1, 5);
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.existsById(2L)).thenReturn(true);
        when(repository.findBenchmarkUnit(2L)).thenReturn(Optional.of("USD/MT"));
        when(repository.findBenchmarkFamily(2L)).thenReturn(Optional.of("Freight"));
        when(repository.findPricesUntil(2L, date)).thenReturn(
            List.of(new PricePoint(date.minusDays(1), new BigDecimal("10")))
        );
        when(repository.findBenchmarkUnit(1L)).thenReturn(Optional.of("USD/BBL"));
        when(repository.findBenchmarkFamily(1L)).thenReturn(Optional.of("Crude Oil (Brut)"));
        when(repository.findPrices(1L, date, date)).thenReturn(
            List.of(new PricePoint(date, new BigDecimal("84")))
        );
    }

    @Test
    void shouldReturnFreightFlagForEachSeries() throws Exception {
        mvc.perform(get("/api/series"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].isFret").value(false))
            .andExpect(jsonPath("$[1].isFret").value(true))
            .andExpect(jsonPath("$[0].logic").value("POL"))
            .andExpect(jsonPath("$[1].logic").value("POD"));
    }

    @Test
    void shouldReturnPricesAndDeltasInRequestedUnit() throws Exception {
        mvc.perform(get("/api/series/1/prices")
                .param("start", "2026-01-05").param("end", "2026-01-05")
                .param("unit", "USD/Gallon"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].price").value(2));

        mvc.perform(get("/api/series/1/deltas")
                .param("start", "2026-01-05").param("end", "2026-01-05")
                .param("referencePrice", "1.9").param("unit", "USD/Gallon"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].delta").value(5.2632));
    }

    @Test
    void shouldReturnPricesAndDeltasWithFreight() throws Exception {
        mvc.perform(get("/api/series/1/prices-with-freight")
                .param("freightId", "2")
                .param("start", "2026-01-05").param("end", "2026-01-05")
                .param("unit", "USD/MT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].price").value(625.72));

        mvc.perform(get("/api/series/1/deltas-with-freight")
                .param("freightId", "2")
                .param("start", "2026-01-05").param("end", "2026-01-05")
                .param("referencePrice", "600").param("unit", "USD/MT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].delta").value(4.2867));
    }

    @Test
    void shouldRequireUnitOnBothEndpoints() throws Exception {
        for (String endpoint : List.of("prices", "deltas")) {
            mvc.perform(get("/api/series/1/" + endpoint)
                    .param("start", "2026-01-05").param("end", "2026-01-05")
                    .param("referencePrice", "550"))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void shouldRejectUnsupportedUnitOnBothEndpoints() throws Exception {
        for (String endpoint : List.of("prices", "deltas")) {
            mvc.perform(get("/api/series/1/" + endpoint)
                    .param("start", "2026-01-05").param("end", "2026-01-05")
                    .param("referencePrice", "550").param("unit", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
    }
}
