package com.semsoft.poc.importdata;

import java.io.File;
import java.sql.Date;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class JsonImportService {

    private final JdbcTemplate jdbc;
    private final JsonMapper mapper;

    public JsonImportService(JdbcTemplate jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void importFile(String filePath) throws Exception {

        JsonNode root = mapper.readTree(new File(filePath));

        JsonNode metadata = root.path("tables").path("metadata");
        JsonNode prices = root.path("tables").path("prices");

        if (!metadata.isArray() || !prices.isArray()) {
            throw new IllegalArgumentException("Invalid dump format");
        }

        System.out.println("Importing metadata...");

        for (JsonNode row : metadata) {
            importMetadata(row);
        }

        System.out.println("Importing prices...");

        for (JsonNode row : prices) {
            importPrice(row);
        }

        System.out.println("Import completed!");
        System.out.println("Metadata: " + metadata.size());
        System.out.println("Prices: " + prices.size());
    }

    private void importMetadata(JsonNode row) {

        String sql = """
            INSERT INTO metadata (
                id, family, zone, logic, benchmark_id,
                reference, expected_unit, bloomberg_ticker,
                series_id, provider, symbol, instrument,
                exchange, currency, unit, price_field,
                roll_rule, qualification, notes, enabled,
                start_date, end_date, last_status, last_error
            )
            VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            ON CONFLICT (id) DO NOTHING
            """;

        jdbc.update(sql,
            row.path("id").asLong(),
            text(row, "family"),
            text(row, "zone"),
            text(row, "logic"),
            text(row, "benchmark_id"),
            text(row, "reference"),
            text(row, "expected_unit"),
            text(row, "bloomberg_ticker"),
            text(row, "series_id"),
            text(row, "provider"),
            text(row, "symbol"),
            text(row, "instrument"),
            text(row, "exchange"),
            text(row, "currency"),
            text(row, "unit"),
            text(row, "price_field"),
            text(row, "roll_rule"),
            text(row, "qualification"),
            text(row, "notes"),
            row.path("enabled").asInt(),
            date(row, "start_date"),
            date(row, "end_date"),
            text(row, "last_status"),
            text(row, "last_error")
        );
    }

    private void importPrice(JsonNode row) {

        String sql = """
            INSERT INTO prices (metadata_id, date, price)
            VALUES (?, ?, ?)
            ON CONFLICT (metadata_id, date)
            DO UPDATE SET price = EXCLUDED.price
            """;

        jdbc.update(sql,
            row.path("metadata_id").asLong(),
            date(row, "date"),
            row.path("price").decimalValue()
        );
    }

    private String text(JsonNode row, String field) {
        JsonNode value = row.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Date date(JsonNode row, String field) {
        String value = text(row, field);
        return value == null ? null : Date.valueOf(value);
    }
}