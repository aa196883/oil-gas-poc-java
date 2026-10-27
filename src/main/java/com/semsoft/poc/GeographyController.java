package com.semsoft.poc;

import java.util.List;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/geography", "/api/gegraphy"})
@Tag(
    name = "Geography",
    description = "Geographic reference data and commodity benchmarks"
)
public class GeographyController {

    private final GeographyService service;

    public GeographyController(GeographyService service) {
        this.service = service;
    }

    @Operation(
        summary = "Get a geographic benchmark",
        description = "Returns the commodity or freight benchmark matching a country and product family."
    )
    @GetMapping("/benchmark")
    public ResponseEntity<?> getBenchmark(
        @Parameter(description = "Country name", example = "France")
        @RequestParam String country,

        @Parameter(description = "Commodity family", example = "Crude Oil")
        @RequestParam String family,

        @Parameter(description = "Whether to return a freight benchmark", example = "false")
        @RequestParam(defaultValue = "false") boolean isFret
    ) {
        return service.getBenchmark(country, family, isFret)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(404).body(new ApiError(
                "BENCHMARK_NOT_FOUND", "No matching " + (isFret ? "freight " : "")
                    + "benchmark for " + family + " in " + country
            )));
    }

    @Operation(
        summary = "List available countries",
        description = "Returns the countries available in the geographic reference data."
    )
    @GetMapping("/countries")
    public List<String> getCountries() {
        return service.getCountries();
    }

    @Operation(
        summary = "List zones for a country",
        description = "Returns the geographic zones available for the requested country."
    )
    @GetMapping("/zones")
    public List<String> getZones(
        @Parameter(description = "Country name", example = "France")
        @RequestParam String country
    ) {
        return service.getZones(country);
    }
}
