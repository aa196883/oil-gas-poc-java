package com.semsoft.poc;

import io.swagger.v3.oas.annotations.media.Schema;

public record SeriesSummary(

    @Schema(description = "Unique identifier of the series", example = "1")
    long id,

    @Schema(description = "Commodity benchmark identifier", example = "wti")
    String benchmarkId,

    @Schema(description = "Corresponding Bloomberg ticker", example = "CL1 Comdty")
    String bloombergTicker,

    @Schema(description = "Data provider", example = "yahoo")
    String provider,

    @Schema(description = "Provider-specific symbol", example = "CL=F")
    String symbol,

    @Schema(description = "Price currency", example = "USD")
    String currency,

    @Schema(description = "Price unit", example = "USD/BBL")
    String unit,

    @Schema(description = "Commodity family", example = "Crude Oil (Brut)")
    String family,

    @Schema(description = "Whether the series contains freight quotations", example = "false")
    boolean isFret,

    @Schema(description = "Shipment price logistic: POL or POD", example = "POL")
    String logic,

    @Schema(description = "Geographical zone", example = "Europe")
    String zone

) {}
