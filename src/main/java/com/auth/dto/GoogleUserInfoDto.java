package com.auth.dto;

public record GoogleUserInfoDto(
        String email,
        String sub,
        boolean emailVerified
) { }
