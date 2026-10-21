package com.semsoft.poc.importdata;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("import")
public class JsonImportRunner implements CommandLineRunner {

    private final JsonImportService importService;
    private final String filePath;

    public JsonImportRunner(
        JsonImportService importService,
        @Value("${import.file}") String filePath
    ) {
        this.importService = importService;
        this.filePath = filePath;
    }

    @Override
    public void run(String... args) throws Exception {
        importService.importFile(filePath);
    }
}