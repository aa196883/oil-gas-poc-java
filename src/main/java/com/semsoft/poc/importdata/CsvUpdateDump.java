package com.semsoft.poc.importdata;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/** Generates and removes disposable price updates used by tests. */
@Service
public class CsvUpdateDump {

    public static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 10, 15);
    public static final Path DUMP_PATH = Path.of("src/main/resources/dump/dump.csv");

    private static final BigDecimal MAX_VARIATION = new BigDecimal("0.10");
    private static final BigDecimal NOISE_VARIATION = new BigDecimal("0.02");

    private final JdbcTemplate jdbc;
    private final Random random;

    public CsvUpdateDump(JdbcTemplate jdbc) {
        this(jdbc, new Random());
    }

    CsvUpdateDump(JdbcTemplate jdbc, Random random) {
        this.jdbc = jdbc;
        this.random = random;
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DumpConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = application.run(args)) {
            CsvUpdateDump dump = context.getBean(CsvUpdateDump.class);
            runCommand(dump, args);
        }
    }

    private static void runCommand(CsvUpdateDump dump, String[] args) {
        if (args.length == 2 && args[0].equals("generate")) {
            try {
                dump.generate(LocalDate.parse(args[1]));
            } catch (IOException exception) {
                throw new IllegalStateException("Could not write " + DUMP_PATH, exception);
            }
            return;
        }
        if (args.length == 1 && args[0].equals("delete")) {
            dump.delete();
            return;
        }
        throw new IllegalArgumentException(
            "Usage: generate <end-date> or delete"
        );
    }

    /** Writes one interpolated, slightly noisy price series per benchmark. */
    public void generate(LocalDate endDate) throws IOException {
        if (endDate == null || endDate.isBefore(CUTOFF_DATE)) {
            throw new IllegalArgumentException("End date must be after the cutoff date");
        }

        List<SeedPrice> rows = jdbc.query("""
            SELECT m.bloomberg_ticker, p.price
            FROM metadata m
            JOIN prices p ON p.metadata_id = m.id
            WHERE m.bloomberg_ticker IS NOT NULL
              AND p.date = (
                SELECT MAX(p2.date)
                FROM prices p2
                WHERE p2.metadata_id = m.id
                  AND p2.date <= ?
            )
            ORDER BY m.id
            """, (rs, rowNum) -> new SeedPrice(
                rs.getString("bloomberg_ticker"),
                rs.getBigDecimal("price")
            ), CUTOFF_DATE);
        Map<String, SeedPrice> seeds = new LinkedHashMap<>();
        for (SeedPrice row : rows) {
            seeds.putIfAbsent(row.bloombergTicker(), row);
        }

        StringBuilder csv = new StringBuilder("bloomberg_ticker,date,price\n");
        for (SeedPrice seed : seeds.values()) {
            BigDecimal target = target(seed.price());
            long days = endDate.toEpochDay() - CUTOFF_DATE.toEpochDay();
            for (long day = 1; day <= days; day++) {
                LocalDate date = CUTOFF_DATE.plusDays(day);
                BigDecimal progress = BigDecimal.valueOf(day)
                    .divide(BigDecimal.valueOf(days), MathContext.DECIMAL128);
                BigDecimal interpolated = seed.price().add(
                    target.subtract(seed.price()).multiply(progress)
                );
                BigDecimal noise = BigDecimal.valueOf(random.nextDouble() * 2 - 1)
                    .multiply(seed.price().abs().multiply(NOISE_VARIATION));
                csv.append(seed.bloombergTicker()).append(',')
                    .append(date).append(',')
                    .append(interpolated.add(noise).max(BigDecimal.ZERO).stripTrailingZeros())
                    .append('\n');
            }
        }

        Files.createDirectories(DUMP_PATH.getParent());
        Files.writeString(DUMP_PATH, csv.toString());
    }

    /** Deletes all disposable prices and prints the number of removed rows. */
    public int delete() {
        int deleted = jdbc.update("DELETE FROM prices WHERE date > ?", CUTOFF_DATE);
        System.out.println("Deleted price rows: " + deleted);
        return deleted;
    }

    private BigDecimal target(BigDecimal seed) {
        double direction = random.nextBoolean() ? 1 : -1;
        double variation = random.nextDouble() * MAX_VARIATION.doubleValue();
        return seed.multiply(BigDecimal.ONE.add(
            BigDecimal.valueOf(direction * variation)
        ));
    }

    private record SeedPrice(String bloombergTicker, BigDecimal price) {}
}

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
class DumpConfiguration {

    @Bean
    CsvUpdateDump csvUpdateDump(JdbcTemplate jdbc) {
        return new CsvUpdateDump(jdbc);
    }
}
