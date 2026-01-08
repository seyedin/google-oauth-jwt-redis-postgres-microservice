package com.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * This record is the Google login request body.
 */
public record GoogleLoginRequestDto(
        @NotBlank String idToken
) { }
