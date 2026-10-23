package com.semsoft.poc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class DeltaService {

    public List<DeltaPoint> getDeltas(
        List<PricePoint> prices,
        BigDecimal referencePrice
    ) {
        if (referencePrice == null || referencePrice.signum() <= 0) {
            throw new IllegalArgumentException("Reference price must be strictly positive");
        }

        return prices.stream()
            .map(price -> new DeltaPoint(
                price.date(),
                price.price()
                    .subtract(referencePrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(referencePrice, 4, RoundingMode.HALF_UP)
            ))
            .toList();
    }
}
