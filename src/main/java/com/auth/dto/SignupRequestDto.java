package com.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * This record is the signup request body.
 */
public record SignupRequestDto(
        @NotBlank String username,
        @NotBlank @Size(min = 4, max = 64) String password
) { }
