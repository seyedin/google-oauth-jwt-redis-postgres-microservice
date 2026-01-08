package com.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * This record is the login request body.
 */
public record LoginRequestDto(
        @NotBlank String username,
        @NotBlank String password
) { }
