package com.semsoft.poc;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UnitConverterTest {

    private final UnitConverter converter = new UnitConverter();

    @Test
    void shouldConvertWtiToUsdPerMt() {
        BigDecimal result = converter.getFactor("USD/BBL", "Crude Oil (Brut)").multiply(new BigDecimal("75.50"));

        assertEquals(0, new BigDecimal("553.4150").compareTo(result));
    }

    @Test
    void shouldReturnLngFreightFactorsByZone() {
        assertEquals(0, new BigDecimal("0.00027228").compareTo(
            converter.getFactor("USD/day", "GNL (LNG)", "Asie")
        ));
        assertEquals(0, new BigDecimal("0.00035008").compareTo(
            converter.getFactor("USD/day", "GNL (LNG)", "Europe")
        ));
        assertEquals(0, new BigDecimal("0.00068719").compareTo(
            converter.getFactor("USD/day", "GNL (LNG)", "USA")
        ));
    }

    @Test
    void shouldRejectLngFreightConversionWithoutKnownZone() {
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.getFactor("USD/day", "GNL (LNG)")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.getFactor("USD/day", "GNL (LNG)", "unknown")
        );
    }

    @Test
    void shouldRejectUnknownConversion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.getFactor("unknown", "Crude Oil (Brut)")
                .multiply(new BigDecimal("100"))
        );
    }
}
