package com.semsoft.poc;

public record SeriesSummary(
    long id,
    String benchmarkId,
    String bloombergTicker,
    String provider,
    String symbol,
    String currency,
    String unit
) {}