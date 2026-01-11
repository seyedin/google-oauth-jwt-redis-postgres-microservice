package com.auth.dto;

import java.util.List;

/**
 * This record is the profile of user.
 * It is sent to the client.
 */
public record UserProfileDto(
        Long id,
        String username,
        List<String> roles
) { }
