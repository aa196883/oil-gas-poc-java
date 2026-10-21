package com.semsoft.poc;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PriceService {

    private final Map<String, CommodityPrice> prices = Map.of(
        "CL1", new CommodityPrice("CL1", "WTI Crude Oil", 75.50, "USD"),
        "BZ1", new CommodityPrice("BZ1", "Brent Crude Oil", 81.25, "USD"),
        "NG1", new CommodityPrice("NG1", "Natural Gas", 3.15, "USD")
    );

    public CommodityPrice getPrice(String ticker) {
        return prices.get(ticker);
    }
}