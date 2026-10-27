package com.semsoft.poc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FreightPricesControllerTest {
    @ParameterizedTest
    @CsvSource({
        "USD/day, Asie, 100000, 27.228",
        "USD/day, Europe, 100000, 35.008",
        "USD/day, USA, 100000, 68.719",
        "USD/MT, Asie, 10, 10"
    })
    void shouldConvertFreightPrices(
        String nativeUnit, String zone, String nativePrice, double freightPrice
    ) throws Exception {
        SeriesRepository repository = mock(SeriesRepository.class);
        LocalDate date = LocalDate.of(2026, 1, 5);
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.existsById(22L)).thenReturn(true);
        when(repository.isFreight(22L)).thenReturn(true);
        when(repository.findBenchmarkUnit(1L)).thenReturn(Optional.of("USD/MT"));
        when(repository.findBenchmarkFamily(1L)).thenReturn(Optional.of("GNL (LNG)"));
        when(repository.findBenchmarkUnit(22L)).thenReturn(Optional.of(nativeUnit));
        when(repository.findBenchmarkFamily(22L)).thenReturn(Optional.of("GNL (LNG)"));
        when(repository.findBenchmarkZone(22L)).thenReturn(Optional.of(zone));
        List<PricePoint> freight = List.of(new PricePoint(date, new BigDecimal(nativePrice)));
        when(repository.findPrices(22L, date, date)).thenReturn(freight);
        when(repository.findPrices(1L, date, date)).thenReturn(
            List.of(new PricePoint(date, new BigDecimal("100")))
        );
        var mvc = MockMvcBuilders.standaloneSetup(new SeriesController(
            new SeriesService(repository, new UnitConverter())
        )).setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(get("/api/series/22/prices")
                .param("start", date.toString()).param("end", date.toString()).param("unit", "USD/MT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].price").value(freightPrice));
        mvc.perform(get("/api/series/22/prices")
                .param("start", date.toString()).param("end", date.toString()).param("unit", "USD/MMBtu"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].price").value(org.hamcrest.Matchers.closeTo(freightPrice / 52, 1e-12)));
    }
}
