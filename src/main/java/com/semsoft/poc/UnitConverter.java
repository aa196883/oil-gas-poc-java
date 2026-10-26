package com.semsoft.poc;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class UnitConverter {
    // TODO : add dynamic eur to usd
    private static final BigDecimal EUR_USD = new BigDecimal("1.12");

    // Approximate family defaults, not cargo-specific density or calorific-value measurements.
    // BP: barrels/tonne and net GJ/tonne for petroleum products; kerosene for Jet Fuel,
    // residual fuel oil for both HSFO and VLSFO, and generic LPG for propane/butane.
    // https://www.bp.com/content/dam/bp/business-sites/en/global/corporate/pdfs/energy-economics/statistical-review/bp-stats-review-2022-approximate-conversion-factors.pdf
    // SEAI: crude oil net calorific value of 42.81 GJ/tonne.
    // https://www.seai.ie/data-and-insights/seai-statistics/conversion-factors
    // LNG volume assumes liquid LNG at 450 kg/m3, not gaseous natural gas:
    // https://www.shell.com.au/content/dam/shell/assets/en/australia/lubricants-for-business/documents/flng-terminal-regulations-rev19-au-en-2024.pdf
    // Preserve the existing LNG energy assumption of 52 MMBtu/tonne (gross basis).
    // US gallon = 3.785411784 litres; barrel = 42 US gallons; cents factors divide dollars by 100.
    // 1 MWh = 3.6 GJ; international-table Btu conversion:
    // https://nvlpubs.nist.gov/nistpubs/Legacy/IR/nbsir81-2401.pdf
    // Factors below are precomputed with the previous DECIMAL64 precision; 1 MMBtu = 1.05505585262 GJ.

    private static final Map<String, Map<String, BigDecimal>> COMMODITY_FACTORS = Map.ofEntries(
        Map.entry("USD/BBL", Map.of(
            "Crude Oil (Brut)", new BigDecimal("7.33"),
            "Gasoline (Essence)", new BigDecimal("8.35"),
            "Gasoil / Diesel", new BigDecimal("7.46"),
            "GPL (Propane / Butane)", new BigDecimal("11.60"),
            "Fuel Oil HSFO", new BigDecimal("6.35"),
            "Fuel Oil VLSFO", new BigDecimal("6.35"),
            "Jet Fuel", new BigDecimal("7.88"),
            "GNL (LNG)", new BigDecimal("13.9773572676269")
        )),
        Map.entry("USD/Gallon", Map.of(
            "Crude Oil (Brut)", new BigDecimal("307.86"),
            "Gasoline (Essence)", new BigDecimal("350.7"),
            "Gasoil / Diesel", new BigDecimal("313.32"),
            "GPL (Propane / Butane)", new BigDecimal("487.2"),
            "Fuel Oil HSFO", new BigDecimal("266.7"),
            "Fuel Oil VLSFO", new BigDecimal("266.7"),
            "Jet Fuel", new BigDecimal("330.96"),
            "GNL (LNG)", new BigDecimal("587.0490052403298")
        )),
        Map.entry("USD/MT", Map.of(
            "Crude Oil (Brut)", BigDecimal.ONE,
            "Gasoline (Essence)", BigDecimal.ONE,
            "Gasoil / Diesel", BigDecimal.ONE,
            "GPL (Propane / Butane)", BigDecimal.ONE,
            "Fuel Oil HSFO", BigDecimal.ONE,
            "Fuel Oil VLSFO", BigDecimal.ONE,
            "Jet Fuel", BigDecimal.ONE,
            "GNL (LNG)", BigDecimal.ONE
        )),
        Map.entry("USD/MMBtu", Map.of(
            "Crude Oil (Brut)", new BigDecimal("40.57605092061311"),
            "Gasoline (Essence)", new BigDecimal("42.41481613402094"),
            "Gasoil / Diesel", new BigDecimal("41.1163066791917"),
            "GPL (Propane / Butane)", new BigDecimal("43.74176010245959"),
            "Fuel Oil HSFO", new BigDecimal("39.4007576914246"),
            "Fuel Oil VLSFO", new BigDecimal("39.4007576914246"),
            "Jet Fuel", new BigDecimal("41.62812792416089"),
            "GNL (LNG)", new BigDecimal("52")
        )),
        Map.entry("USD/MWh", Map.of(
            "Crude Oil (Brut)", new BigDecimal("11.89166666666667"),
            "Gasoline (Essence)", new BigDecimal("12.43055555555556"),
            "Gasoil / Diesel", new BigDecimal("12.05"),
            "GPL (Propane / Butane)", new BigDecimal("12.81944444444444"),
            "Fuel Oil HSFO", new BigDecimal("11.54722222222222"),
            "Fuel Oil VLSFO", new BigDecimal("11.54722222222222"),
            "Jet Fuel", new BigDecimal("12.2"),
            "GNL (LNG)", new BigDecimal("15.23969564895556")
        ))
    );

    // LNG freight factors convert USD/day into USD/MT for a standard LNG cargo.
    // They assume a 174,000 m3 tanker, LNG density of 0.45 tonne/m3, and average
    // tanker fullness of 98.5%. Travel durations include the return journey:
    // 21 days for Australia -> Japan (Asia), 27 days for US Gulf -> Europe,
    // and 53 days for US Gulf -> Japan (USA).
    // factor = round-trip duration / (174,000 * 0.45 * 0.985)
    private static final Map<String, BigDecimal> FRET_FACTORS = Map.of(
        "Asie", new BigDecimal("0.00027228"),
        "Europe", new BigDecimal("0.00035008"),
        "USA", new BigDecimal("0.00068719")
    );

    public BigDecimal getCommodityFactor(String unit, String family) {
        BigDecimal currencyFactor = BigDecimal.ONE;
        String normalizedUnit = unit;
        if (unit != null && unit.startsWith("EUR/")) {
            currencyFactor = EUR_USD;
            normalizedUnit = "USD/" + unit.substring(4);
        } else if (unit != null && unit.startsWith("US Cents/")) {
            currencyFactor = new BigDecimal("0.01");
            normalizedUnit = "USD/" + unit.substring(9);
        }
        Map<String, BigDecimal> familyFactors = COMMODITY_FACTORS.get(normalizedUnit == null ? "" : normalizedUnit);
        BigDecimal factor = familyFactors == null || family == null ? null : familyFactors.get(family);

        if (factor == null) {
            throw new IllegalArgumentException("Unknown commodity conversion: " + unit + " / " + family);
        }

        return currencyFactor.multiply(factor);
    }

    public BigDecimal getFreightFactor(String unit, String family, String zone) {
        if ("USD/MT".equals(unit)) {
            return BigDecimal.ONE;
        }

        BigDecimal factor = "USD/day".equals(unit) && "GNL (LNG)".equals(family) && zone != null
            ? FRET_FACTORS.get(zone)
            : null;

        if (factor == null) {
            throw new IllegalArgumentException(
                "Unknown freight conversion: " + unit + " / " + family + " / " + zone
            );
        }

        return factor;
    }
}
