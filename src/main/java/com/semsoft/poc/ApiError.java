package com.semsoft.poc;

public record ApiError(
    String code,
    String message
) {}