package com.semsoft.poc;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PriceControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PriceController(new PriceService()))
        .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void shouldTransformPrices() throws Exception {
        mvc.perform(post("/api/prices/transform").contentType(MediaType.APPLICATION_JSON).content("""
            {"prices":[{"date":"2026-10-01","price":500}],
             "operations":[{"type":"ADD_FREIGHT","prices":[{"date":"2026-09-30","price":30}]},
                           {"type":"MULTIPLY","value":1.10},{"type":"ADD","value":20}]}
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].date").value("2026-10-01"))
            .andExpect(jsonPath("$[0].price").value(603));
    }

    @Test
    void shouldRejectInvalidRequests() throws Exception {
        for (String body : new String[] {
            "{}", "{\"prices\":[],\"operations\":[{\"type\":\"UNKNOWN\"}]}",
            "{\"prices\":[],\"operations\":[{\"type\":\"MULTIPLY\"}]}",
            "{\"prices\":[{\"date\":\"2026-10-01\"}],\"operations\":[]}",
            "{\"prices\":[],\"operations\":[null]}"
        }) {
            mvc.perform(post("/api/prices/transform").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
        mvc.perform(post("/api/prices/transform").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
    }
}
