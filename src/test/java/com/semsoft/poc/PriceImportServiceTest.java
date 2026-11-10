package com.semsoft.poc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceImportServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PriceImportService service = new PriceImportService(jdbc);

    @Test
    void shouldRejectMalformedCsvWithClearMessage() {
        InvalidImportDataException exception = assertThrows(
            InvalidImportDataException.class,
            () -> importCsv("ticker,date,price\nCL1 Comdty,2026-10-16,100\n")
        );

        assertTrue(exception.getMessage().contains("bloomberg_ticker,date,price"));
    }

    @Test
    void shouldRejectUnknownTickerWithClearMessage() {
        when(jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM metadata WHERE bloomberg_ticker = ?)",
            Boolean.class,
            "UNKNOWN"
        )).thenReturn(false);

        InvalidImportDataException exception = assertThrows(
            InvalidImportDataException.class,
            () -> importCsv("bloomberg_ticker,date,price\nUNKNOWN,2026-10-16,100\n")
        );

        assertTrue(exception.getMessage().contains("Bloomberg ticker"));
    }

    @Test
    void shouldRejectDuplicateTickerAndDateRowsWithClearMessage() {
        when(jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM metadata WHERE bloomberg_ticker = ?)",
            Boolean.class,
            "CL1 Comdty"
        )).thenReturn(true);

        InvalidImportDataException exception = assertThrows(
            InvalidImportDataException.class,
            () -> importCsv("""
                bloomberg_ticker,date,price
                CL1 Comdty,2026-10-16,100
                CL1 Comdty,2026-10-16,101
                """)
        );

        assertTrue(exception.getMessage().contains("duplicate ticker/date"));
    }

    private void importCsv(String csv) throws Exception {
        service.importPrices(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }
}
