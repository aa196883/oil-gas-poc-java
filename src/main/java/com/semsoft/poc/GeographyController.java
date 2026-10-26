package com.semsoft.poc;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geography")
public class GeographyController {

    private final GeographyService service;

    public GeographyController(GeographyService service) {
        this.service = service;
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