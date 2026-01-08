package com.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig creates Redis beans for the Auth Service.
 *
 * <p>This configuration reads Redis connection values from environment variables
 * via Spring Boot properties (application.yml placeholders).
 *
 * <p>Why we do this:
 * - Do not hardcode secrets in code
 * - Make the service runnable in different environments (local, docker, k8s)
 */
@Configuration
public class RedisConfig {

    /**
     * Redis host name (example: localhost).
     */
    @Value("${redis.host:localhost}")
    private String host;

    /**
     * Redis port (default: 6379).
     */
    @Value("${redis.port:6379}")
    private int port;

    /**
     * Redis password (optional).
     * If empty or blank, no password is set.
     */
    @Value("${redis.password:}")
    private String password;

    /**
     * Creates a RedisConnectionFactory based on RedisStandaloneConfiguration.
     *
     * @return RedisConnectionFactory for Lettuce client
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);

        if (password != null && !password.isBlank()) {
            config.setPassword(RedisPassword.of(password));
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * Creates RedisTemplate for simple String key/value usage.
     *
     * <p>This project uses RedisTemplate for:
     * - allow-list keys: auth:allowlist:&lt;token&gt;
     * - blacklist keys: auth:blacklist:&lt;token&gt;
     *
     * @param connectionFactory redis connection factory
     * @return RedisTemplate<String, String>
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }
}
