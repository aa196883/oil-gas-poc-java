package com.semsoft.poc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        byte[] contents = csv.readAllBytes();
        if (!isCorrectFormat(new ByteArrayInputStream(contents))) {
            throw new IllegalArgumentException("Invalid CSV format");
        }
        if (!hasCoherentTickers(new ByteArrayInputStream(contents))) {
            throw new IllegalArgumentException("CSV contains an unknown or empty Bloomberg ticker");
        }
        if (!hasValidDates(new ByteArrayInputStream(contents))) {
            throw new IllegalArgumentException("CSV contains an invalid date or price");
        }

        int count = 0;

        try (BufferedReader reader = reader(new ByteArrayInputStream(contents))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = columns(line);
                String ticker = columns[0].trim();
                LocalDate date = LocalDate.parse(columns[1].trim());
                BigDecimal price = new BigDecimal(columns[2].trim());

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

    public boolean isCorrectFormat(InputStream csv) {
        try (BufferedReader reader = reader(csv)) {
            if (!"bloomberg_ticker,date,price".equals(reader.readLine())) {
                return false;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = columns(line);
                if (columns.length != 3) {
                    return false;
                }
                if (!columns[1].isBlank()) {
                    LocalDate.parse(columns[1].trim());
                }
                if (!columns[2].isBlank()) {
                    new BigDecimal(columns[2].trim());
                }
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public boolean hasCoherentTickers(InputStream csv) {
        try (BufferedReader reader = reader(csv)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String ticker = columns(line)[0].trim();
                if (ticker.isEmpty() || !jdbc.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM metadata WHERE bloomberg_ticker = ?)",
                    Boolean.class,
                    ticker
                )) {
                    return false;
                }
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public boolean hasValidDates(InputStream csv) {
        try (BufferedReader reader = reader(csv)) {
            reader.readLine();
            Set<String> incomingRows = new HashSet<>();
            int overlaps = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = columns(line);
                if (columns[1].isBlank() || columns[2].isBlank()) {
                    return false;
                }
                LocalDate date = LocalDate.parse(columns[1].trim());
                new BigDecimal(columns[2].trim());
                String key = columns[0].trim() + "\u0000" + date;
                if (!incomingRows.add(key)) {
                    return false;
                }
                overlaps += jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM prices p
                    JOIN metadata m ON m.id = p.metadata_id
                    WHERE m.bloomberg_ticker = ?
                      AND p.date = ?
                    """, Integer.class, columns[0].trim(), date);
            }
            if (overlaps > 0) {
                System.out.println("Overlapping price rows: " + overlaps);
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private BufferedReader reader(InputStream csv) {
        return new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8));
    }

    private String[] columns(String line) {
        return line.split(",", -1);
    }
}
