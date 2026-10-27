package com.semsoft.poc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertNull;

class SeriesSummaryTest {

    @ParameterizedTest
    @CsvSource({
        ", Orient",
        "Jet Fuel,",
        "Unknown, Orient",
        "Jet Fuel, Unknown"
    })
    void shouldAllowMissingOrUnmappedMetadata(String family, String zone) {
        SeriesSummary summary = new SeriesSummary(
            1L, "test", null, "test", null, "USD", "USD/MT",
            family, false, "POL", zone
        );

        assertNull(summary.margins());
        assertNull(summary.marginsWithFreight());
    }
}
