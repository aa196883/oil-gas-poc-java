package com.semsoft.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "Oil & Gas Price API",
        version = "1.0",
        description = "API for accessing historical Oil & Gas commodity prices."
    )
)
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}