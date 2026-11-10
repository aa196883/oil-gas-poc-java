package com.semsoft.poc;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PriceImportControllerTest {

    private final PriceImportService service = mock(PriceImportService.class);
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new PriceImportController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @Test
    void shouldExposeInvalidImportDataAsBadRequest() throws Exception {
        when(service.importPrices(any(InputStream.class)))
            .thenThrow(new InvalidImportDataException("Invalid CSV format"));

        mvc.perform(multipart("/api/import/prices")
                .file(new MockMultipartFile("file", "prices.csv", "text/csv", "bad".getBytes())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_IMPORT_DATA"))
            .andExpect(jsonPath("$.message").value("Invalid CSV format"));
    }
}
