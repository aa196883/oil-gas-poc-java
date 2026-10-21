package com.semsoft.poc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController 
public class PriceController {
    
    @GetMapping("/api/hello")
    public String hello() {
        return "Hello Oil & Gas !";
    }

    @GetMapping("/api/price")
    public CommodityPrice getPrice() {
        return new CommodityPrice(
            "CL1",
            "WTI Crude Oil",
            75.50,
            "USD"
        );
    }

    @GetMapping("/api/prices/{ticker}")
    public CommodityPrice getPriceByTicker(@PathVariable String ticker) {
        return new CommodityPrice(
            ticker,
            "Unknown commodity",
            100.0,
            "USD"
        );
    }
}
