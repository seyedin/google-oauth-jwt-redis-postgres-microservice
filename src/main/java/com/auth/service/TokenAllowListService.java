package com.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenAllowListService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private String key(String token) {
        return "auth:allowlist:" + token;
    }

    public void add(String token, String username) {
        redisTemplate.opsForValue().set(
                key(token),
                username,
                jwtExpirationMs,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isAllowed(String token) {
        Boolean hasKey = redisTemplate.hasKey(key(token));
        return Boolean.TRUE.equals(hasKey);
    }

    public void remove(String token) {
        redisTemplate.delete(key(token));
    }
}
