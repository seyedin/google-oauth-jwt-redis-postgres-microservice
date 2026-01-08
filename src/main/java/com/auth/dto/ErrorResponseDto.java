package com.auth.dto;

/**
 * This record is the error response.
 */
public record ErrorResponseDto(
        int status,
        String message
) { }
