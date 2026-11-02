package com.semsoft.poc.alert;

import com.semsoft.poc.AnalysisService;
import com.semsoft.poc.GlobalExceptionHandler;
import com.semsoft.poc.PriceService;
import com.semsoft.poc.SeriesService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlertControllerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AlertController(new AlertService(
        new AnalysisService(mock(SeriesService.class), new PriceService()))))
        .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void shouldReturnAllTriggeredAlerts() throws Exception {
        mvc.perform(post("/api/alerts").contentType(MediaType.APPLICATION_JSON).content("""
            {"prices":[{"date":"2026-01-01","price":99},{"date":"2026-01-02","price":100},
                       {"date":"2026-01-03","price":101}],
             "declaredPrice":120,"lowerBounds":[],"upperBounds":[]}
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].type").value("MEAN_GAP"))
            .andExpect(jsonPath("$[0].severity").value("high"))
            .andExpect(jsonPath("$[0].message").isString())
            .andExpect(jsonPath("$[1].type").value("TIME_OUTSIDE_MARGINS"))
            .andExpect(jsonPath("$[2].type").value("ZSCORE"))
            .andExpect(jsonPath("$[3].type").value("ABOVE_Q95"))
            .andExpect(jsonPath("$[3].severity").value("medium"));
    }

    @Test
    void shouldReturnEmptyListWhenNoRuleTriggers() throws Exception {
        mvc.perform(post("/api/alerts").contentType(MediaType.APPLICATION_JSON).content("""
            {"prices":[{"date":"2026-01-01","price":100}],"declaredPrice":100,
             "lowerBounds":[{"date":"2026-01-01","price":90}],
             "upperBounds":[{"date":"2026-01-01","price":110}]}
            """))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void shouldRejectInvalidInputs() throws Exception {
        for (String body : List.of("{}",
            "{\"prices\":[],\"declaredPrice\":100,\"lowerBounds\":[],\"upperBounds\":[]}",
            "{\"prices\":[null],\"declaredPrice\":100,\"lowerBounds\":[],\"upperBounds\":[]}",
            """
            {"prices":[{"date":"2026-01-01","price":100}],"declaredPrice":0,
             "lowerBounds":[],"upperBounds":[]}
            """,
            """
            {"prices":[{"date":"2026-01-01","price":100}],"declaredPrice":100,"upperBounds":[]}
            """)) {
            mvc.perform(post("/api/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
    }
}
