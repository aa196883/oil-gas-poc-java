package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricePoint(
    LocalDate date,
    BigDecimal price
) {}