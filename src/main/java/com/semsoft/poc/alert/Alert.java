package com.semsoft.poc.alert;

public record Alert(
    String type,
    String severity,
    String message
) {}
