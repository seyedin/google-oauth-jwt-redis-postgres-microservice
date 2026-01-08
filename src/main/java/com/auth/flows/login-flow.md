# ==========================================
# Login Implementation Flow (auth)
# ==========================================

This document describes the **exact login flow** as implemented in the project,
based strictly on the provided PDF and existing source code.

---

## Login Flow – Table of Contents

0. Preconditions  
1. Controller endpoint (`/auth/login`)  
2. Request validation (`LoginRequestDto`)  
3. AuthService.login (business logic)  
4. AuthenticationManager & UserDetailsService  
5. JWT generation  
6. Refresh token handling  
7. Redis allow-list handling  
8. Response  
9. Error handling  
10. Test flow  

---

## 0. Preconditions

The following components already exist and are reused:

- `User` entity implements `UserDetails`
- `UserRepository`
- `UserDetailsService` (loads user by username)
- `AuthenticationManager`
- `PasswordEncoder` (BCrypt)
- `JwtTokenProvider`
- `RefreshToken` entity + repository
- Redis allow-list service
- Stateless Spring Security configuration

---

## 1. Controller endpoint (`/auth/login`)

- Endpoint: `POST /auth/login`
- Request body: `LoginRequestDto`
- Uses `@Valid`
- Delegates to `authService.login(request)`
- Returns **200 OK** with `AuthResponseDto`

---

## 2. Request validation (`LoginRequestDto`)

Fields:
- `username`
- `password`

Validations:
- `@NotBlank` on both fields

Validation behavior:
- DTO validation is handled by Spring before entering service logic.
- Validation errors result in `MethodArgumentNotValidException`
  handled by `GlobalExceptionHandler` → **400 Bad Request**.

---

## 3. AuthService.login (business logic)

Method:
```

login(LoginRequestDto request)

```

### Steps

1. Call `authenticationManager.authenticate(...)` with:
   - `UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())`
2. If credentials are invalid:
   - `AuthenticationManager` throws `AuthenticationException`
3. On successful authentication:
   - authenticated `Authentication` object is returned
4. Extract authenticated user (`UserDetails`)
5. Generate access token (JWT)
6. Create refresh token (DB)
7. Store access token in Redis allow-list
8. Return `AuthResponseDto`

Note:
- No manual null/blank validation is performed in the service.
- All input validation is done at DTO level.

---

## 4. AuthenticationManager & UserDetailsService

- `AuthenticationManager.authenticate(...)`:
  - delegates user lookup to `UserDetailsService`
- `UserDetailsService`:
  - loads user from database via `UserRepository.findByUsername`
- Password verification:
  - handled internally using `BCryptPasswordEncoder`

If credentials are invalid:
- `AuthenticationException` is thrown by Spring Security.

---

## 5. JWT generation

Access token is generated using `JwtTokenProvider`:

- Subject: `username`
- Claim: `role`
- Expiration: `jwt.expiration`
- Signing algorithm: HS256
- Secret key derived from `jwt.secret`

---

## 6. Refresh token handling

During login:

- A **new refresh token** is created
- Stored in `refresh_tokens` table
- Properties:
  - random UUID value
  - expiry date = now + **30 days**
  - `revoked = false`

Notes:
- Existing refresh tokens are **not revoked** during login.
- Refresh tokens are revoked **per token** when they are used
  in the `/auth/refresh` flow (one-time use refresh token).

---

## 7. Redis allow-list handling

After successful login:

- Access token is added to Redis allow-list

Details:
- Key format: `auth:allowlist:<accessToken>`
- Value: `username`
- TTL: same as `jwt.expiration`

Implementation note:
- Redis connection parameters are currently **hardcoded in `RedisConfig`**.

---

## 8. Response

Returned object:

```

AuthResponseDto(
tokenType = "Bearer",
accessToken,
refreshToken,
username
)

```

Controller response:
- HTTP **200 OK**
- Body: `AuthResponseDto`

---

## 9. Error handling

### Validation errors
- Exception: `MethodArgumentNotValidException`
- Source: DTO validation
- Response: **400 Bad Request**

### Authentication failure
- Exception: `AuthenticationException`
- Source: `AuthenticationManager`
- Response: **401 Unauthorized**
- Message: `"Invalid username or password"`

### Unauthorized access to protected endpoints
- Handled by `JwtAuthEntryPoint`
- Response: **401 Unauthorized**
- Message: `"Invalid token"`

### Other runtime errors
- Not explicitly handled and may result in default error response.

---

## 10. Test Flow

### 10.1 Successful login
- Valid username and password
- Expect:
  - HTTP **200**
  - accessToken present
  - refreshToken present
  - Redis allow-list key exists

### 10.2 Invalid credentials
- Wrong username or password
- Expect:
  - HTTP **401 Unauthorized**

### 10.3 Validation error
- Blank username or password
- Expect:
  - HTTP **400 Bad Request**

---

End of Login Flow

---
