package com.semsoft.poc;

import java.util.List;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/geography", "/api/gegraphy"})
public class GeographyController {

    private final GeographyService service;

    public GeographyController(GeographyService service) {
        this.service = service;
    }

    @GetMapping("/benchmark")
    public ResponseEntity<?> getBenchmark(
        @RequestParam String country,
        @RequestParam String family,
        @RequestParam(defaultValue = "false") boolean isFret
    ) {
        return service.getBenchmark(country, family, isFret)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(404).body(new ApiError(
                "BENCHMARK_NOT_FOUND", "No matching " + (isFret ? "freight " : "")
                    + "benchmark for " + family + " in " + country
            )));
    }

    @GetMapping("/countries")
    public List<String> getCountries() {
        return service.getCountries();
    }

    @GetMapping("/zones")
    public List<String> getZones(@RequestParam String country) {
        return service.getZones(country);
    }
}