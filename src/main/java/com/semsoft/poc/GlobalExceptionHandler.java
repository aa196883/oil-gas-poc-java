package com.semsoft.poc;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SeriesNotFoundException.class)
    public ResponseEntity<ApiError> handleSeriesNotFound(
        SeriesNotFoundException exception
    ) {
        ApiError error = new ApiError(
            "SERIES_NOT_FOUND",
            exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidArgument(IllegalArgumentException exception) {

        ApiError error = new ApiError(
            "INVALID_ARGUMENT",
            exception.getMessage()
        );

        return ResponseEntity.badRequest().body(error);
    }
}