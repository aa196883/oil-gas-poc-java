package com.semsoft.poc;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GeographyControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        SeriesService seriesService = mock(SeriesService.class);
        when(seriesService.getAllSeries()).thenReturn(List.of(
            series(1, "Crude Oil (Brut)", "Monde", false),
            series(2, "Crude Oil (Brut)", "Europe / Afrique", false),
            series(3, "Crude Oil (Brut)", "Europe", false),
            series(4, "Crude Oil (Brut)", "Orient", false),
            series(5, "Crude Oil (Brut)", "Europe", true),
            series(6, "Gasoline (Essence)", "Reste du Monde", false),
            series(7, "Gasoline (Essence)", "Amériques", false),
            series(8, "Crude Oil (Brut)", "Amériques", false),
            series(9, "Crude Oil (Brut)", "USA", false)
        ));
        mvc = MockMvcBuilders.standaloneSetup(new GeographyController(
            new GeographyService(JsonMapper.builder().build(), seriesService)
        )).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private SeriesSummary series(long id, String family, String zone, boolean freight) {
        return new SeriesSummary(id, "test", null, "test", null, "USD", "USD/MT",
            family, freight, "POL", zone);
    }

    @Test
    void shouldPreferSpecificZonesAndSeparateFreight() throws Exception {
        for (var example : List.of(
            new String[]{"France", "3"}, new String[]{"Oman", "4"},
            new String[]{"United States of America", "9"}, new String[]{"Australia", "1"},
            new String[]{"Algeria", "2"}
        )) {
            mvc.perform(get("/api/geography/benchmark")
                    .param("country", example[0]).param("family", "Crude Oil (Brut)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(example[1])));
        }
        mvc.perform(get("/api/gegraphy/benchmark")
                .param("country", "France").param("family", "Crude Oil (Brut)")
                .param("isFret", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(5));
        mvc.perform(get("/api/geography/benchmark")
                .param("country", "France").param("family", "Gasoline (Essence)"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(6));
    }

    @Test
    void shouldReportInvalidContextAndMissingBenchmarks() throws Exception {
        mvc.perform(get("/api/geography/benchmark").param("country", "France"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/geography/benchmark")
                .param("country", "unknown").param("family", "Crude Oil (Brut)"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/geography/benchmark")
                .param("country", "France").param("family", ""))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/geography/benchmark")
                .param("country", "Oman").param("family", "Crude Oil (Brut)")
                .param("isFret", "true"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("BENCHMARK_NOT_FOUND"));
    }
}
