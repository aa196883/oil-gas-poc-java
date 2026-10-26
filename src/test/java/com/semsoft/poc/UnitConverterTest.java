package com.semsoft.poc;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UnitConverterTest {

    private final UnitConverter converter = new UnitConverter();

    @Test
    void shouldConvertCrudeOilFromEurPerMtToUsdPerMt() {
        BigDecimal factor = converter.getCommodityFactor("EUR/MT", "Crude Oil (Brut)");

        assertEquals(0, new BigDecimal("1.12").compareTo(factor));
        assertEquals(0, new BigDecimal("112.00").compareTo(
            new BigDecimal("100").multiply(factor)
        ));
    }

    @Test
    void shouldConvertWtiToUsdPerMt() {
        BigDecimal result = converter.getCommodityFactor("USD/BBL", "Crude Oil (Brut)").multiply(new BigDecimal("75.50"));

        assertEquals(0, new BigDecimal("553.4150").compareTo(result));
    }

    @Test
    void shouldReturnLngFreightFactorsByZone() {
        assertEquals(0, new BigDecimal("0.00027228").compareTo(
            converter.getFreightFactor("USD/day", "GNL (LNG)", "Asie")
        ));
        assertEquals(0, new BigDecimal("0.00035008").compareTo(
            converter.getFreightFactor("USD/day", "GNL (LNG)", "Europe")
        ));
        assertEquals(0, new BigDecimal("0.00068719").compareTo(
            converter.getFreightFactor("USD/day", "GNL (LNG)", "USA")
        ));
    }

    @Test
    void shouldReturnIdentityFactorForFreightAlreadyInUsdPerMt() {
        assertEquals(
            0,
            BigDecimal.ONE.compareTo(converter.getFreightFactor("USD/MT", "Crude Oil (Brut)", null))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.getFreightFactor("USD/day", "GNL (LNG)", "unknown")
        );
    }

    @Test
    void shouldCombineCurrencyAndQuantityFactorsForEveryFamily() {
        for (String family : java.util.List.of("Crude Oil (Brut)", "Gasoline (Essence)",
                "Gasoil / Diesel", "GPL (Propane / Butane)", "Fuel Oil HSFO",
                "Fuel Oil VLSFO", "Jet Fuel", "GNL (LNG)")) {
            for (String quantity : java.util.List.of("MT", "BBL", "Gallon", "MMBtu", "MWh")) {
                BigDecimal usdFactor = converter.getCommodityFactor("USD/" + quantity, family);
                assertEquals(0, usdFactor.multiply(new BigDecimal("1.12")).compareTo(
                    converter.getCommodityFactor("EUR/" + quantity, family)));
                assertEquals(0, usdFactor.multiply(new BigDecimal("0.01")).compareTo(
                    converter.getCommodityFactor("US Cents/" + quantity, family)));
            }
        }
        assertEquals(0, new BigDecimal("12.2").compareTo(
            converter.getCommodityFactor("USD/MWh", "Jet Fuel")));
        assertEquals(0, new BigDecimal("13.664").compareTo(
            converter.getCommodityFactor("EUR/MWh", "Jet Fuel")));
        assertEquals(0, new BigDecimal("8.2096").compareTo(
            converter.getCommodityFactor("EUR/BBL", "Crude Oil (Brut)")));
    }

    @Test
    void shouldRejectUnknownConversion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.getCommodityFactor("unknown", "Crude Oil (Brut)")
                .multiply(new BigDecimal("100"))
        );
    }
}
