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
import static org.mockito.Mockito.times;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        AnalysisService analysisService = new AnalysisService(seriesService, new PriceService());
        mvc = MockMvcBuilders.standaloneSetup(new AnalysisController(analysisService, new DeltaService()))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnMarginFlagsAndRejectInvalidRequests() throws Exception {
        mvc.perform(post("/api/analysis/within-margins")
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"prices":[{"date":"2026-01-01","price":10},{"date":"2026-01-02","price":10}],
                     "lowerBounds":[{"date":"2026-01-01","price":10}],
                     "upperBounds":[{"date":"2026-01-01","price":20}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value(true))
            .andExpect(jsonPath("$[1]").value(false));
        for (String body : List.of("{}", """
            {"prices":[{"date":"2026-01-01"}],"lowerBounds":[],"upperBounds":[]}
            """)) {
            mvc.perform(post("/api/analysis/within-margins")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
    }

    @Test
    void shouldReturnMeanAndRejectEmptyPricesOrMissingParameters() throws Exception {
        LocalDate date = LocalDate.of(2026, 1, 1);
        when(seriesService.getPrices(1L, date, date, "USD/MT"))
            .thenReturn(List.of(new PricePoint(date, new BigDecimal("10"))));
        when(seriesService.getPrices(2L, LocalDate.of(1, 1, 1), date, "USD/MT"))
            .thenReturn(List.of(new PricePoint(date, new BigDecimal("3"))));
        for (boolean withFreight : new boolean[] {false, true}) {
            var request = get("/api/analysis/1/mean").param("start", "2026-01-01")
                .param("end", "2026-01-01").param("unit", "USD/MT");
            if (withFreight) request.param("freightId", "2");
            mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$").value(withFreight ? 13 : 10));
        }
        when(seriesService.getPrices(1L, date, date, "USD/MT")).thenReturn(List.of());
        mvc.perform(get("/api/analysis/1/mean").param("start", "2026-01-01")
                .param("end", "2026-01-01").param("unit", "USD/MT"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/analysis/1/mean")).andExpect(status().isBadRequest());
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
        when(seriesService.getPrices(15L, LocalDate.of(1, 1, 1), end, "USD/MT"))
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

        verify(seriesService, times(2)).getPrices(1L, start, end, "USD/MT");
        verify(seriesService).getPrices(15L, LocalDate.of(1, 1, 1), end, "USD/MT");
    }

    @Test
    void shouldCalculateDeltasFromSubmittedPrices() throws Exception {
        mvc.perform(post("/api/analysis/deltas").contentType(MediaType.APPLICATION_JSON).content("""
            {"prices":[{"date":"2026-10-01","price":550}],"referencePrice":500}
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].date").value("2026-10-01"))
            .andExpect(jsonPath("$[0].delta").value(10));
    }

    @Test
    void shouldRejectMissingPricesAndInvalidReferencePrice() throws Exception {
        for (String body : new String[] {
            "{}", "{\"prices\":[],\"referencePrice\":0}",
            "{\"prices\":[null],\"referencePrice\":500}",
            "{\"prices\":[],\"referencePrice\":-1}"
        }) {
            mvc.perform(post("/api/analysis/deltas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
    }
}
