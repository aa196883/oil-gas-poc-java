package com.semsoft.poc.alert;

import com.semsoft.poc.AnalysisService;
import com.semsoft.poc.PricePoint;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    public static final BigDecimal MAX_TOLERATE_GAP_FROM_AVG = new BigDecimal("0.15");
    public static final BigDecimal MAX_TIME_OUTSIED_MARGINS = new BigDecimal("0.50");
    public static final BigDecimal MAX_ABSOLUTE_ZSCORE = new BigDecimal("3.5");

    private final AnalysisService analysisService;

    public AlertService(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public List<Alert> evaluate(
        List<PricePoint> prices, BigDecimal declaredPrice,
        List<PricePoint> lowerBounds, List<PricePoint> upperBounds
    ) {
        BigDecimal mean = analysisService.computeMean(prices);
        BigDecimal zscore = analysisService.computeZscore(prices, declaredPrice);
        List<PricePoint> declaredPrices = prices.stream()
            .map(point -> new PricePoint(point.date(), declaredPrice)).toList();
        List<Boolean> withinMargins = analysisService.getWithinMargins(declaredPrices, lowerBounds, upperBounds);
        Map<Double, BigDecimal> quantiles = analysisService.computeQuantiles(prices);
        List<Alert> alerts = new ArrayList<>();

        // The relative gap is undefined when the market mean is zero.
        if (mean.signum() != 0) {
            BigDecimal gap = declaredPrice.subtract(mean).abs().divide(mean.abs(), MathContext.DECIMAL128);
            if (gap.compareTo(MAX_TOLERATE_GAP_FROM_AVG) > 0) {
                alerts.add(new Alert("MEAN_GAP", "high",
                    "The declared price is " + format(gap.movePointRight(2)) + "% "
                        + (declaredPrice.compareTo(mean) > 0 ? "above" : "below")
                        + " the average market price of " + format(mean) + " (limit: 15%)."));
            }
        }

        long outsideCount = withinMargins.stream().filter(within -> !within).count();
        BigDecimal outsideFraction = BigDecimal.valueOf(outsideCount)
            .divide(BigDecimal.valueOf(withinMargins.size()), MathContext.DECIMAL128);
        if (outsideFraction.compareTo(MAX_TIME_OUTSIED_MARGINS) > 0) {
            alerts.add(new Alert("TIME_OUTSIDE_MARGINS", "high",
                "The declared price is outside the tolerated margins for "
                    + format(outsideFraction.movePointRight(2)) + "% of observations ("
                    + outsideCount + " of " + withinMargins.size() + "; limit: 50%)."));
        }

        if (zscore != null && zscore.abs().compareTo(MAX_ABSOLUTE_ZSCORE) > 0) {
            alerts.add(new Alert("ZSCORE", "high",
                "The declared price is unusually far from the market median, given the observed price dispersion."));
        }

        if (declaredPrice.compareTo(quantiles.get(0.95)) > 0) {
            alerts.add(new Alert("ABOVE_Q95", "medium",
                "The declared price is above the 95th percentile of market prices ("
                    + format(quantiles.get(0.95)) + ")."));
        } else if (declaredPrice.compareTo(quantiles.get(0.05)) < 0) {
            alerts.add(new Alert("BELOW_Q05", "medium",
                "The declared price is below the 5th percentile of market prices ("
                    + format(quantiles.get(0.05)) + ")."));
        }
        return alerts;
    }

    private static String format(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
