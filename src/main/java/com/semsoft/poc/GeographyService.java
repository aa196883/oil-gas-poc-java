package com.semsoft.poc;

import java.io.IOException;
import java.util.*;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GeographyService {

    private static final Set<String> ORIENT = Set.of(
        "SA", "AE", "OM", "QA", "KW", "BH",
        "IQ", "IR", "YE", "JO", "LB", "SY",
        "IL", "PS", "EG", "TR"
    );

    private final Map<String, JsonNode> countries;

    public GeographyService(JsonMapper mapper) throws IOException {
        try (var input = new ClassPathResource("geography/countries.json").getInputStream()) {
            JsonNode json = mapper.readTree(input);

            countries = new HashMap<>();
            for (JsonNode country : json) {
                countries.put(country.get("name").asText(), country);
            }
        }
    }

    public List<String> getCountries() {
        return countries.keySet().stream().sorted().toList();
    }

    public List<String> getZones(String countryName) {
        JsonNode country = countries.get(countryName);

        if (country == null) {
            throw new IllegalArgumentException("Unknown country: " + countryName);
        }

        String code = country.get("code").asText();
        String region = country.get("region").asText();

        List<String> zones = new ArrayList<>();

        if (code.equals("US")) {
            zones.add("USA");
        }

        if (ORIENT.contains(code)) {
            zones.add("Orient");
        }

        switch (region) {
            case "Europe" -> {
                zones.add("Europe");
                zones.add("Europe / Afrique");
            }
            case "Africa" -> zones.add("Europe / Afrique");
            case "Americas" -> zones.add("Amériques");
            case "Asia" -> zones.add("Asie");
        }

        zones.add("Monde");

        return zones;
    }
}