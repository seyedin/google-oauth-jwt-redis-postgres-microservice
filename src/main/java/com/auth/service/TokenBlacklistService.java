package com.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * This service saves revoked tokens in Redis.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    private String key(String token) {
        return "auth:blacklist:" + token;
    }

    /**
     * This method adds a token to blacklist.
     *
     * @param token the access token string
     * @param expiresAt the time when token expires
     */
    public void add(String token, Instant expiresAt) {
        long ttlMillis = Duration.between(Instant.now(), expiresAt).toMillis();
        if (ttlMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue()
                .set(key(token), "revoked", ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * This method checks if token is in blacklist.
     *
     * @param token the access token string
     * @return true when token is revoked
     */
    public boolean isBlacklisted(String token) {
        Boolean hasKey = redisTemplate.hasKey(key(token));
        return Boolean.TRUE.equals(hasKey);
    }
}
