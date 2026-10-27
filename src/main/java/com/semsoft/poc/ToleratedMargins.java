package com.semsoft.poc;

import java.util.Map;

public final class ToleratedMargins {

    public static final Map<String, Map<String, double[]>> MARGINS = Map.ofEntries(
        Map.entry("Crude Oil (Brut)", marginsForZones(
            new double[] {-0.15, 0.10},
            "Amériques", "Europe / Afrique", "Orient"
        )),
        Map.entry("Gasoil / Diesel", marginsForZones(
            new double[] {-0.02, 0.05},
            "Amériques", "Europe / Afrique", "Orient"
        )),
        Map.entry("Gasoline (Essence)", Map.of(
            "Amériques", new double[] {-0.03, 0.06},
            "Monde", new double[] {-0.08, 0.10}
        )),
        Map.entry("Fuel Oil HSFO", marginsForZones(
            new double[] {-0.05, 0.08},
            "Europe / Afrique", "Orient"
        )),
        Map.entry("Fuel Oil VLSFO", marginsForZones(
            new double[] {-0.03, 0.06},
            "Amériques", "Europe / Afrique", "Orient"
        )),
        Map.entry("Jet Fuel", Map.of(
            "Orient", new double[] {-0.03, 0.06},
            "Europe / Afrique", new double[] {-0.08, 0.10}
        )),
        Map.entry("GPL (Propane / Butane)", Map.of(
            "Monde", new double[] {-0.05, 0.08}
        )),
        Map.entry("GNL (LNG)", marginsForZones(
            new double[] {-0.10, 0.10},
            "Asie", "Europe", "USA"
        ))
    );

    public static final Map<String, Map<String, double[]>> MARGINS_WITH_FREIGHTS = Map.ofEntries(
        Map.entry("Crude Oil (Brut)", marginsForZones(
            new double[] {-0.20, 0.15},
            "Amériques", "Europe / Afrique", "Orient"
        )),
        Map.entry("Gasoil / Diesel", marginsForZones(
            new double[] {-0.07, 0.10},
            "Amériques", "Europe / Afrique", "Orient"
        )),
        Map.entry("Gasoline (Essence)", Map.of(
            "Amériques", new double[] {-0.08, 0.11},
            "Monde", new double[] {-0.12, 0.15}
        )),
        Map.entry("Fuel Oil HSFO", marginsForZones(
            new double[] {-0.10, 0.13},
            "Europe / Afrique", "Orient"
        )),
        Map.entry("Fuel Oil VLSFO", marginsForZones(
            new double[] {-0.08, 0.11},
            "Amériques", "Europe / Afrique", "Orient"
        )),
        Map.entry("Jet Fuel", Map.of(
            "Orient", new double[] {-0.08, 0.11},
            "Europe / Afrique", new double[] {-0.12, 0.15}
        )),
        Map.entry("GPL (Propane / Butane)", Map.of(
            "Monde", new double[] {-0.06, 0.10}
        )),
        Map.entry("GNL (LNG)", marginsForZones(
            new double[] {-0.10, 0.15},
            "Asie", "Europe", "USA"
        ))
    );

    private ToleratedMargins() {
    }

    public static double[] getCommodityMargins(String family, String zone) {
        return getMargins(MARGINS, family, zone);
    }

    public static double[] getCommodityMarginsWithFreight(String family, String zone) {
        return getMargins(MARGINS_WITH_FREIGHTS, family, zone);
    }

    private static double[] getMargins(
        Map<String, Map<String, double[]>> margins,
        String family,
        String zone
    ) {
        Map<String, double[]> familyMargins = margins.get(family);
        return familyMargins == null ? null : familyMargins.get(zone);
    }

    private static Map<String, double[]> marginsForZones(
        double[] margins,
        String... zones
    ) {
        Map<String, double[]> marginsByZone = new java.util.HashMap<>();
        for (String zone : zones) {
            marginsByZone.put(zone, margins.clone());
        }
        return Map.copyOf(marginsByZone);
    }
}
