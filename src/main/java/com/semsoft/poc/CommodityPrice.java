package com.semsoft.poc;

public record CommodityPrice(
    String ticker,
    String commodity,
    double price,
    String currency
) {} 
