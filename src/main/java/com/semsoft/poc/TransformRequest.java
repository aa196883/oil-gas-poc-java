package com.semsoft.poc;

import java.util.List;

public record TransformRequest(List<PricePoint> prices, List<Operation> operations) {}
