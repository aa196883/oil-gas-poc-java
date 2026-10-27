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
            new SeriesService(repository, converter)
        )).setControllerAdvice(new GlobalExceptionHandler()).build();

        when(repository.findAll()).thenReturn(List.of(
            new SeriesSummary(1L, "wti", null, "test", null, "USD", "USD/BBL", "Crude Oil (Brut)", false, "POL", "Amériques"),
            new SeriesSummary(2L, "freight", null, "test", null, "USD", "USD/MT", "Freight", true, "POD", "Europe")
        ));

        LocalDate date = LocalDate.of(2026, 1, 5);
        when(repository.existsById(1L)).thenReturn(true);
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
            .andExpect(jsonPath("$[1].logic").value("POD"))
            .andExpect(jsonPath("$[0].zone").value("Amériques"))
            .andExpect(jsonPath("$[0].margins[0]").value(-0.15))
            .andExpect(jsonPath("$[0].margins[1]").value(0.10))
            .andExpect(jsonPath("$[0].marginsWithFreight[0]").value(-0.20))
            .andExpect(jsonPath("$[0].marginsWithFreight[1]").value(0.15))
            .andExpect(jsonPath("$[1].margins").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$[1].marginsWithFreight").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$[1].zone").value("Europe"));
    }

    @Test
    void shouldReturnPricesInRequestedUnit() throws Exception {
        mvc.perform(get("/api/series/1/prices")
                .param("start", "2026-01-05").param("end", "2026-01-05")
                .param("unit", "USD/Gallon"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].price").value(2));

    }

    @Test
    void shouldRequireUnit() throws Exception {
        mvc.perform(get("/api/series/1/prices")
                .param("start", "2026-01-05").param("end", "2026-01-05"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnsupportedUnit() throws Exception {
        mvc.perform(get("/api/series/1/prices")
                .param("start", "2026-01-05").param("end", "2026-01-05")
                .param("unit", "unknown"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void shouldRemoveCombinationEndpoints() throws Exception {
        for (String endpoint : List.of("ajusted-prices", "prices-with-freight", "deltas", "deltas-with-freight")) {
            mvc.perform(get("/api/series/1/" + endpoint))
                .andExpect(status().isNotFound());
        }
    }
}
