package com.semsoft.poc;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/series")
@Tag(
    name = "Price Series",
    description = "Historical commodity price series"
)
public class SeriesController {

    private final SeriesService seriesService;

    public SeriesController(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    @Operation(
        summary = "List available price series",
        description = "Returns all available commodity price series and their metadata."
    )
    @GetMapping
    public List<SeriesSummary> getAllSeries() {
        return seriesService.getAllSeries();
    }

    @Operation(
        summary = "Get historical prices",
        description = "Converts historical observations through USD/MT into the requested unit using the series family."
    )
    @GetMapping("/{id}/prices")
    public List<PricePoint> getPrices(
        @Parameter(description = "Unique identifier of the price series", example = "1")
        @PathVariable long id,

        @Parameter(description = "Start date (inclusive)", example = "2020-01-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,

        @Parameter(description = "End date (inclusive)", example = "2020-01-31")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,

        @Parameter(description = "Unit of returned prices", example = "USD/BBL")
        @RequestParam String unit
    ) {
        return seriesService.getPrices(id, start, end, unit);
    }

}
