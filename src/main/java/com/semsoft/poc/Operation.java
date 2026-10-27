package com.semsoft.poc;

import java.math.BigDecimal;
import java.util.List;

public record Operation(String type, BigDecimal value, List<PricePoint> prices) {}
