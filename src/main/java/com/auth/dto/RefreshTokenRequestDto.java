package com.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * This record is the refresh token request body.
 */
public record RefreshTokenRequestDto(
        @NotBlank String refreshToken
) { }
