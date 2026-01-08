package com.auth.controller;

import com.auth.dto.*;
import com.auth.model.User;
import com.auth.repository.RefreshTokenRepository;
import com.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * This controller has auth endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * This endpoint creates a new user.
     *
     * @param request signup data
     * @return token and username
     */
    @Operation(summary = "Signup new user", description = "Create a new account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Username already used")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequestDto request) {
        log.info("Signup request received for username: {}", request.username());

        AuthResponseDto response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * This endpoint logs in the user.
     *
     * @param request login data
     * @return token and username
     */
    @Operation(summary = "Login user", description = "Authenticate user and return JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request) {

        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    @Operation(summary = "Login with Google",
            description = "Authenticate user using Google id_token and return JWT token")
    public ResponseEntity<AuthResponseDto> googleLogin(
            @Valid @RequestBody GoogleLoginRequestDto request) {

        AuthResponseDto response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(response);
    }

    /**
     * This endpoint returns current user profile.
     *
     * @param user the current user from security
     * @return profile data
     */
    @Operation(summary = "Get current user", description = "Return profile of logged-in user")
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(@AuthenticationPrincipal User user) {
        UserProfileDto profile = authService.me(user);
        return ResponseEntity.ok(profile);
    }

    /**
     * This endpoint logs out the user.
     * It revokes the current access token.
     *
     * @param user    the current user
     * @param request the http request
     * @return empty response
     */
    @Operation(summary = "Logout user", description = "Logout current user and revoke access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logout success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user,
                                       HttpServletRequest request) {

        if (user != null) {
            String authHeader = request.getHeader("Authorization");
            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            log.info("Logout request received for username: {}", user.getUsername());
            authService.logout(user, token);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * This endpoint creates new access token from refresh token.
     *
     * @param request the refresh token data
     * @return new access and refresh token
     */
    @Operation(summary = "Refresh access token", description = "Create new tokens from refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        AuthResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
