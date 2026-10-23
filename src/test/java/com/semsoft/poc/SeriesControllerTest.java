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
            new DeltaService(repository, converter)
        )).setControllerAdvice(new GlobalExceptionHandler()).build();

        LocalDate date = LocalDate.of(2026, 1, 5);
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.findBenchmarkUnit(1L)).thenReturn(Optional.of("USD/BBL"));
        when(repository.findBenchmarkFamily(1L)).thenReturn(Optional.of("Crude Oil (Brut)"));
        when(repository.findPrices(1L, date, date)).thenReturn(
            List.of(new PricePoint(date, new BigDecimal("84")))
        );
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
