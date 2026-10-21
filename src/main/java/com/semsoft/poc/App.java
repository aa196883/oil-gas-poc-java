package com.semsoft.poc;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;

public class App {

    public static void main(String[] args) throws Exception {

        CommodityPrice oil = new CommodityPrice(
            "CL1",
            "WTI Crude Oil",
            75.50,
            "USD"
        );

        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(oil);

        System.out.println(json);

        String pricesJson = """
            [
                {"ticker": "CL1", "commodity": "WTI", "price": 75.5, "currency": "USD"},
                {"ticker": "BZ1", "commodity": "Brent", "price": 81.25, "currency": "USD"},
                {"ticker": "NG1", "commodity": "Natural Gas", "price": 3.15, "currency": "USD"}
            ]
            """;

        List<CommodityPrice> prices = mapper.readValue(
            pricesJson,
            new TypeReference<List<CommodityPrice>>() {}
        );

        for (CommodityPrice price : prices) {
            System.out.println(price.ticker() + " : " + price.price());
        }
    }
}