package com.semsoft.poc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/api/prices/{ticker}")
    public CommodityPrice getPriceByTicker(@PathVariable String ticker) {
        return priceService.getPrice(ticker);
    }
}