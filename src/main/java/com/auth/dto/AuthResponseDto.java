package com.auth.dto;

/**
 * This record is the auth response.
 * It returns access and refresh token to the client.
 */
public record AuthResponseDto(
        String tokenType,
        String accessToken,
        String refreshToken,
        String username
) { }
