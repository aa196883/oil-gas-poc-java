package com.semsoft.poc;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/import")
@Tag(name = "Price Import", description = "Import Bloomberg price quotations")
public class PriceImportController {

    private final PriceImportService importService;

    public PriceImportController(PriceImportService importService) {
        this.importService = importService;
    }

    @Operation(summary = "Import price quotations from a CSV file")
    @PostMapping(value = "/prices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public int importPrices(@RequestParam("file") MultipartFile file) throws IOException {
        return importService.importPrices(file.getInputStream());
    }
}