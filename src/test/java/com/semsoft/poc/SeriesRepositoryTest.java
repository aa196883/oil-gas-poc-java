package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
public class SeriesRepositoryTest {

    @Container
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SeriesRepository repository;

    @Test
    void shouldRetrieveFreightFlag() {
        jdbc.update("""
            INSERT INTO metadata (id, benchmark_id, series_id, is_fret)
            VALUES (1000, 'test-commodity', 'test-series-1000', FALSE),
                   (1001, 'test-freight', 'test-series-1001', TRUE)
            """);

        List<SeriesSummary> series = repository.findAll();
        assertFalse(series.stream().filter(item -> item.id() == 1000L).findFirst().orElseThrow().isFret());
        assertTrue(series.stream().filter(item -> item.id() == 1001L).findFirst().orElseThrow().isFret());
    }

    @Test
    void shouldRetrievePricesFromDatabase() {

        jdbc.update("""
            INSERT INTO metadata (id, benchmark_id, series_id)
            VALUES (?, ?, ?)
            """,
            1000L, "test-wti", "test-series-1000"
        );

        jdbc.update("""
            INSERT INTO prices (metadata_id, date, price)
            VALUES (?, ?, ?)
            """,
            1000L,
            LocalDate.of(2026, 1, 5),
            new BigDecimal("75.50")
        );

        List<PricePoint> prices = repository.findPrices(
            1000L,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31)
        );

        assertEquals(1, prices.size());
        assertEquals(
            LocalDate.of(2026, 1, 5),
            prices.get(0).date()
        );
        assertEquals(
            0,
            new BigDecimal("75.50").compareTo(prices.get(0).price())
        );
    }
}