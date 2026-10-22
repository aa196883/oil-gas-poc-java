package com.semsoft.poc;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class UnitConverter {

    private static final BigDecimal EUR_USD = new BigDecimal("1.12");

    private static final Map<String, BigDecimal> FACTORS = Map.ofEntries(
        // Crude Oil
        Map.entry("oman", new BigDecimal("7.33")),
        Map.entry("wti", new BigDecimal("7.33")),
        Map.entry("brent", new BigDecimal("7.33")),

        // Diesel
        Map.entry("ulsd", new BigDecimal("3.129")),

        // Gasoline
        Map.entry("rbob", new BigDecimal("3.57")),

        // Propane
        Map.entry("propane", new BigDecimal("5.208")),

        // Gasoil
        Map.entry("gasoil-singapore", BigDecimal.ONE),
        Map.entry("gasoil-ice", BigDecimal.ONE),

        // HSFO
        Map.entry("hsfo-singapore", BigDecimal.ONE),
        Map.entry("hsfo-us", BigDecimal.ONE),
        Map.entry("hsfo-rotterdam", BigDecimal.ONE),

        // VLSFO
        Map.entry("vlsfo-singapore", BigDecimal.ONE),
        Map.entry("vlsfo-us", BigDecimal.ONE),
        Map.entry("vlsfo-rotterdam", BigDecimal.ONE),

        // Natural Gas / LNG
        Map.entry("jkm", new BigDecimal("52")),
        Map.entry("henry-hub", new BigDecimal("52")),

        // TTF: EUR/MWh -> USD/MT
        Map.entry(
            "ttf",
            new BigDecimal("15.2396972").multiply(EUR_USD)
        )
    );

    public BigDecimal getFactor(String benchmarkId) {
        BigDecimal factor = FACTORS.get(benchmarkId);

        if (factor == null) {
            throw new IllegalArgumentException("Unknown benchmark: " + benchmarkId);
        }

        return factor;
    }
}