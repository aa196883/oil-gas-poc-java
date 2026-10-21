package com.semsoft.poc;

import org.springframework.stereotype.Service;

@Service
public class PriceService {

    public CommodityPrice getPrice(String ticker) {
        return new CommodityPrice(
            ticker,
            "Unknown commodity",
            100.0,
            "USD"
        );
    }
}