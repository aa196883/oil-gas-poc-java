package com.semsoft.poc;

public class PriceNotFoundException extends RuntimeException {

    private final String ticker;

    public PriceNotFoundException(String ticker) {
        super("Price not found for ticker: " + ticker);
        this.ticker = ticker;
    }

    public String getTicker() {
        return ticker;
    }
    
}
