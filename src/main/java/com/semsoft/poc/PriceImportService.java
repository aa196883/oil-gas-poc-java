package com.semsoft.poc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceImportService {

    private final JdbcTemplate jdbc;

    public PriceImportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(rollbackFor = Exception.class)
    public int importPrices(InputStream csv) throws IOException {
        int count = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csv, StandardCharsets.UTF_8))) {

            reader.readLine(); // Skip CSV header

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");

                String ticker = columns[0];
                LocalDate date = LocalDate.parse(columns[1]);
                BigDecimal price = new BigDecimal(columns[2]);

                List<Long> metadataIds = jdbc.query(
                    "SELECT id FROM metadata WHERE bloomberg_ticker = ?",
                    (rs, rowNum) -> rs.getLong("id"),
                    ticker
                );

                if (metadataIds.isEmpty()) {
                    throw new IllegalArgumentException("Unknown Bloomberg ticker: " + ticker);
                }
                for (Long metadataId : metadataIds) {
                    jdbc.update(
                        """
                        INSERT INTO prices (metadata_id, date, price)
                        VALUES (?, ?, ?)
                        ON CONFLICT (metadata_id, date)
                        DO UPDATE SET price = EXCLUDED.price
                        """,
                        metadataId, date, price
                    );
                }

                count++;
            }
        }

        return count;
    }
}
