package com.auth.service;

import com.auth.dto.*;
import com.auth.exception.RefreshTokenException;
import com.auth.model.RefreshToken;
import com.auth.model.Role;
import com.auth.model.User;
import com.auth.repository.RefreshTokenRepository;
import com.auth.repository.RoleRepository;
import com.auth.repository.UserRepository;
import com.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * This class has the auth business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TokenAllowListService tokenAllowListService;
    private final TokenBlacklistService tokenBlacklistService;
    private final GoogleAuthService googleAuthService;


    /**
     * This method creates a new user.
     *
     * @param username the username
     * @param password the password
     * @return auth response with token
     */
    public AuthResponseDto signup(String username, String password, String roleName) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (username.length() < 3 || username.length() > 100) {
            throw new IllegalArgumentException("Username must be between 3 and 100 characters");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role must not be blank");
        }

        if (userRepository.existsByUsername(username)) {
            log.warn("Signup failed. Username already used: {}", username);
            throw new IllegalStateException("Username is already used");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " not found"));

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .provider("LOCAL")
                .roles(Set.of(role))
                .build();

        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = createRefreshToken(user);
        tokenAllowListService.add(accessToken, user.getUsername());

        log.info("Signup success. New user created: {}", user.getUsername());
        return new AuthResponseDto("Bearer", accessToken, refreshToken, user.getUsername());
    }

    /**
     * This method creates a new user from dto.
     *
     * @param request signup dto
     * @return auth response with token
     */
    public AuthResponseDto signup(SignupRequestDto request) {
        return signup(request.username(), request.password(), request.role());
    }

    /**
     * This method logs in the user.
     *
     * @param request login dto
     * @return auth response with token
     */
    public AuthResponseDto login(LoginRequestDto request) {

        Authentication authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                );

        Authentication authentication =
                authenticationManager.authenticate(authenticationToken);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = (User) userDetails;

        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = createRefreshToken(user);
        tokenAllowListService.add(accessToken, user.getUsername());

        log.info("Login success for user: {}", user.getUsername());
        return new AuthResponseDto("Bearer", accessToken, refreshToken, user.getUsername());
    }

    /**
     * This method creates new access token from refresh token.
     *
     * @param request refresh token request
     * @return auth response with new tokens
     */
    public AuthResponseDto refreshToken(RefreshTokenRequestDto request) {

        String token = request.refreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new RefreshTokenException("Refresh token is revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RefreshTokenException("Refresh token is expired");
        }

        User user = refreshToken.getUser();

// just one time use refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtTokenProvider.generateToken(user);
        String newRefreshToken = createRefreshToken(user);
        tokenAllowListService.add(newAccessToken, user.getUsername());

        log.info("Refresh token success for user: {}", user.getUsername());
        return new AuthResponseDto("Bearer", newAccessToken, newRefreshToken, user.getUsername());
    }

    /**
     * This method logs out the user.
     * It revokes the access token in Redis.
     *
     * @param user        the current user
     * @param accessToken the current access token
     */
    public void logout(User user, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Logout called with empty token for user: {}",
                    user != null ? user.getUsername() : "unknown");
            return;
        }

        Date expirationDate = jwtTokenProvider.getExpiration(accessToken);
        Instant expiresAt = expirationDate.toInstant();
        if (expiresAt.isBefore(Instant.now())) {
            log.info("Logout called with already expired token for user: {}",
                    user.getUsername());
            return;
        }

        // remove token from allow-list
        tokenAllowListService.remove(accessToken);

        // add token to blacklist in Redis
        tokenBlacklistService.add(accessToken, expiresAt);

        log.info("Logout success. Access token revoked in Redis for user: {}",
                user.getUsername());
    }

    /**
     * This method creates a new refresh token for user.
     *
     * @param user the user
     * @return refresh token string
     */
    private String createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    /**
     * This method makes login with Google.
     * It takes Google idToken.
     * It checks idToken.
     * If user not exist, it makes new Google user.
     * It sets username = email.
     * It makes random password and hash it.
     * It makes access token and refresh token.
     */
    public AuthResponseDto loginWithGoogle(GoogleLoginRequestDto request) {

        // check Google id token
        GoogleUserInfoDto googleUser = googleAuthService.verifyIdToken(request.idToken());

        // find user by email and provider GOOGLE
        Optional<User> optionalUser =
                userRepository.findByEmailAndProvider(googleUser.email(), "GOOGLE");

        User user = optionalUser.orElseGet(() -> createGoogleUser(googleUser));

        // make JWT tokens
        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = createRefreshToken(user);

        // add to allow-list
        tokenAllowListService.add(accessToken, user.getUsername());

        log.info("Google login success for user: {}", user.getUsername());

        return new AuthResponseDto("Bearer", accessToken, refreshToken, user.getUsername());
    }

    /**
     * This method makes new Google user from Google data.
     * It sets username = email.
     * It sets provider = GOOGLE.
     * It makes random password (strong).
     * It hashes password.
     */
    private User createGoogleUser(GoogleUserInfoDto googleUser) {

        String randomPassword = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(randomPassword);

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

        User newUser = User.builder()
                .username(googleUser.email())
                .email(googleUser.email())
                .password(encodedPassword)
                .provider("GOOGLE")
                .providerId(googleUser.sub())
                .roles(Set.of(userRole))
                .build();

        return userRepository.save(newUser);
    }

    /**
     * This method makes strong random password.
     * It uses big letters, small letters, numbers.
     */
    private String generateRandomPassword() {
        int length = 16;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyz"
                + "0123456789";

        SecureRandom random = new SecureRandom();

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            password.append(chars.charAt(index));
        }

        return password.toString();
    }


    public UserProfileDto me(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                roles
        );
    }

}
