package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DeltaPoint(
    LocalDate date,
    BigDecimal delta
) {}