package com.semsoft.poc;

public class SeriesNotFoundException extends RuntimeException {

    public SeriesNotFoundException(long seriesId) {
        super("Series not found: " + seriesId);
    }
}