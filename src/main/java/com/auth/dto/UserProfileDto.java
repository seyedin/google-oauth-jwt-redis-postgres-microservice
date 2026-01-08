package com.auth.dto;

/**
 * This record is the profile of user.
 * It is sent to the client.
 */
public record UserProfileDto(
        Long id,
        String username,
        String role
) { }
