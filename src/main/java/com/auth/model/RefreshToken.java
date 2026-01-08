package com.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * This class is the refresh token entity.
 * It is stored in database.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    /**
     * This is the id of refresh token.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * This is the token string.
     */
    @Column(nullable = false, unique = true, length = 200)
    private String token;

    /**
     * This is the user of token.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * This is the time when token expires.
     */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * This is true when token is revoked.
     */
    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * This is the time when token is created.
     */
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
