package com.auth.repository;

import com.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * This interface is the refresh token repository.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * This method finds refresh token by string.
     *
     * @param token the refresh token string
     * @return optional refresh token
     */
    Optional<RefreshToken> findByToken(String token);

}
