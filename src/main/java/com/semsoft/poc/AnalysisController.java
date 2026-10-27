package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Price Analysis", description = "Statistical analysis of commodity prices")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final DeltaService deltaService;

    public AnalysisController(AnalysisService analysisService, DeltaService deltaService) {
        this.analysisService = analysisService;
        this.deltaService = deltaService;
    }

    public record DeltaRequest(List<PricePoint> prices, BigDecimal referencePrice) {}

    @PostMapping("/deltas")
    public List<DeltaPoint> getDeltas(@RequestBody DeltaRequest request) {
        PriceService.validatePrices(request.prices());
        return deltaService.getDeltas(request.prices(), request.referencePrice());
    }

    @Operation(
        summary = "Compute price quantiles",
        description = "Computes price quantiles, optionally including freight, in the requested unit."
    )
    @GetMapping("/{id}/quantiles")
    public Map<Double, BigDecimal> getQuantiles(
        @PathVariable long id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
        @RequestParam String unit,
        @RequestParam(required = false) Long freightId
    ) {
        return analysisService.getQuantiles(id, start, end, unit, freightId);
    }
}
