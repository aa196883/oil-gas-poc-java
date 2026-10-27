package com.semsoft.poc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToleratedMarginsTest {

    @Test
    void shouldReturnMarginsUsingDatabaseFamilyAndZoneNames() {
        assertArrayEquals(
            new double[] {-0.15, 0.10},
            ToleratedMargins.getCommodityMargins("Crude Oil (Brut)", "Orient")
        );
        assertArrayEquals(
            new double[] {-0.08, 0.10},
            ToleratedMargins.getCommodityMargins("Gasoline (Essence)", "Monde")
        );
        assertArrayEquals(
            new double[] {-0.08, 0.10},
            ToleratedMargins.getCommodityMargins("Jet Fuel", "Europe / Afrique")
        );
        assertArrayEquals(
            new double[] {-0.10, 0.15},
            ToleratedMargins.getCommodityMarginsWithFreight("GNL (LNG)", "USA")
        );
    }

    @Test
    void shouldReturnNullForUnknownCommodityOrZone() {
        assertNull(ToleratedMargins.getCommodityMargins("Unknown", "Orient"));
        assertNull(ToleratedMargins.getCommodityMargins("Jet Fuel", "Unknown"));
    }
}
