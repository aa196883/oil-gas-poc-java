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
    void shouldRejectUnknownConversion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.getFactor("unknown", "Crude Oil (Brut)")
                .multiply(new BigDecimal("100"))
        );
    }
}
