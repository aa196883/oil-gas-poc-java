package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalysisControllerTest {

    private MockMvc mvc;
    private SeriesService seriesService;

    @BeforeEach
    void setUp() {
        seriesService = mock(SeriesService.class);
        AnalysisService analysisService = new AnalysisService(seriesService);
        mvc = MockMvcBuilders.standaloneSetup(new AnalysisController(analysisService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnQuantilesWithOptionalFreight() throws Exception {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        List<PricePoint> prices = List.of(
            new PricePoint(start, new BigDecimal("10")),
            new PricePoint(end, new BigDecimal("20"))
        );
        when(seriesService.getPrices(1L, start, end, "USD/MT")).thenReturn(prices);
        when(seriesService.getPricesWithFreight(1L, 15L, start, end, "USD/MT"))
            .thenReturn(prices);

        mvc.perform(get("/api/analysis/1/quantiles")
                .param("start", "2026-01-01")
                .param("end", "2026-01-31")
                .param("unit", "USD/MT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['0.05']").value(10.5))
            .andExpect(jsonPath("$['0.5']").value(15.0))
            .andExpect(jsonPath("$['0.95']").value(19.5));

        mvc.perform(get("/api/analysis/1/quantiles")
                .param("start", "2026-01-01")
                .param("end", "2026-01-31")
                .param("unit", "USD/MT")
                .param("freightId", "15"))
            .andExpect(status().isOk());

        verify(seriesService).getPrices(1L, start, end, "USD/MT");
        verify(seriesService).getPricesWithFreight(1L, 15L, start, end, "USD/MT");
    }
}
