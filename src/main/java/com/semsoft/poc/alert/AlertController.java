package com.semsoft.poc.alert;

import com.semsoft.poc.PricePoint;
import io.swagger.v3.oas.annotations.Operation;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    public record AlertRequest(
        List<PricePoint> prices, BigDecimal declaredPrice,
        List<PricePoint> lowerBounds, List<PricePoint> upperBounds
    ) {}

    @Operation(summary = "Evaluate all declared-price alerts",
        description = "Supply adjusted market prices, a positive declared price, and dated lower/upper bounds "
            + "in the same unit. Returns all triggered alerts, or an empty list. Bounds are inclusive; "
            + "missing bounds count as outside margins. Zero mean skips the relative-gap rule; "
            + "zero MAD skips the Z-score rule.")
    @PostMapping
    public List<Alert> evaluate(@RequestBody AlertRequest request) {
        return alertService.evaluate(request.prices(), request.declaredPrice(),
            request.lowerBounds(), request.upperBounds());
    }
}
