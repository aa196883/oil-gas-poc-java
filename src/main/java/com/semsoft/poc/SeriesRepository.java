package com.semsoft.poc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SeriesRepository {

    private final JdbcTemplate jdbc;

    public SeriesRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SeriesSummary> findAll() {

        String sql = """
            SELECT id, benchmark_id, bloomberg_ticker,
                   provider, symbol, currency, unit
            FROM metadata
            ORDER BY id
            """;

        return jdbc.query(sql, (rs, rowNum) ->
            new SeriesSummary(
                rs.getLong("id"),
                rs.getString("benchmark_id"),
                rs.getString("bloomberg_ticker"),
                rs.getString("provider"),
                rs.getString("symbol"),
                rs.getString("currency"),
                rs.getString("unit")
            )
        );
    }

    public List<PricePoint> findPrices(
        long seriesId,
        LocalDate start,
        LocalDate end
    ) {
        String sql = """
            SELECT date, price
            FROM prices
            WHERE metadata_id = ?
            AND date >= ?
            AND date <= ?
            ORDER BY date
            """;

        return jdbc.query(
            sql,
            (rs, rowNum) -> new PricePoint(
                rs.getDate("date").toLocalDate(),
                rs.getBigDecimal("price")
            ),
            seriesId,
            start,
            end
        );
    }

    public boolean existsById(long seriesId) {

        String sql = """
            SELECT COUNT(*)
            FROM metadata
            WHERE id = ?
            """;

        Long count = jdbc.queryForObject(
            sql,
            Long.class,
            seriesId
        );

        return count != null && count > 0;
    }

    public Optional<String> findBenchmarkId(long seriesId) {
        String sql = "SELECT benchmark_id FROM metadata WHERE id = ?";

        List<String> results = jdbc.query(
            sql,
            (rs, rowNum) -> rs.getString("benchmark_id"),
            seriesId
        );

        return results.stream().findFirst();
    }

}