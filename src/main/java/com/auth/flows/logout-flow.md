# ==========================================
# Logout Implementation Flow (auth)
# ==========================================

This document describes the **exact logout flow** as implemented in the project,
based strictly on the provided PDF and existing source code.

---

## Logout Flow – Table of Contents

0. Preconditions  
1. Controller endpoint (`/auth/logout`)  
2. Authentication requirement (`@AuthenticationPrincipal`)  
3. Extract access token from `Authorization` header  
4. AuthService.logout (business logic)  
5. Redis allow-list removal  
6. Redis blacklist add (with TTL)  
7. Response  
8. Error handling  
9. Test flow  

---

## 0. Preconditions

The following components already exist and are reused:

- Stateless Spring Security configuration (protected endpoint)
- `JwtAuthFilter` reads `Authorization: Bearer <token>`
- `JwtTokenProvider.getExpiration(token)`
- Redis allow-list service: `TokenAllowListService`
- Redis blacklist service: `TokenBlacklistService`

Redis keys:
- allow-list: `auth:allowlist:<token>`
- blacklist: `auth:blacklist:<token>`

---

## 1. Controller endpoint (`/auth/logout`)

- Endpoint: `POST /auth/logout`
- Authentication: **required** (endpoint is protected by Spring Security)
- Reads current user via `@AuthenticationPrincipal`
- Reads access token from `Authorization` header
- Delegates to `authService.logout(user, token)`
- Returns **204 No Content**

---

## 2. Authentication requirement (`@AuthenticationPrincipal`)

- Endpoint is protected by Spring Security.
- If request is **unauthenticated**, controller is **not executed** and
  `JwtAuthEntryPoint` returns **401 Unauthorized**.
- If authenticated, `@AuthenticationPrincipal` always contains a valid `User`.

Controller signature:
```java
logout(@AuthenticationPrincipal User user, HttpServletRequest request)
````

---

## 3. Extract access token from `Authorization` header

Steps in controller:

1. Read header:

  * `String authHeader = request.getHeader("Authorization")`
2. If header starts with `"Bearer "`:

  * `token = authHeader.substring(7)`
3. If header missing or not Bearer:

  * `token` remains null

Controller logs:

* `Logout request received for username: <username>`

---

## 4. AuthService.logout (business logic)

Method:

```
logout(User user, String accessToken)
```

### Steps

1. If `accessToken` is null or blank:

  * log warning
  * return (do nothing)
2. Read token expiration:

  * `Date expirationDate = jwtTokenProvider.getExpiration(accessToken)`
  * `Instant expiresAt = expirationDate.toInstant()`
3. If token already expired (`expiresAt` is before now):

  * log info
  * return (do nothing)
4. Remove token from allow-list:

  * `tokenAllowListService.remove(accessToken)`
5. Add token to blacklist in Redis:

  * `tokenBlacklistService.add(accessToken, expiresAt)`
6. Log success:

  * `Logout success. Access token revoked in Redis for user: <username>`

---

## 5. Redis allow-list removal

* Key format: `auth:allowlist:<token>`
* Removal uses:

  * `tokenAllowListService.remove(token)`
* Internally it calls:

  * `redisTemplate.delete(key(token))`

---

## 6. Redis blacklist add (with TTL)

* Key format: `auth:blacklist:<token>`
* Value: `"revoked"`
* TTL is calculated:

  * `Duration.between(now, expiresAt).toMillis()`
* If TTL <= 0:

  * service returns without writing key

Blacklist add uses:

* `tokenBlacklistService.add(token, expiresAt)`

---

## 7. Response

* Controller always returns:

  * **204 No Content**
* No response body

---

## 8. Error handling

### Unauthorized (no/invalid token on protected endpoint)

* Handled by `JwtAuthEntryPoint`
* Response: **401 Unauthorized**
* Message: `"Invalid token"`

### Other runtime errors

* Not explicitly handled and may result in default error response.

---

## 9. Test Flow

### 9.1 Successful logout

* Call `POST /auth/logout` with valid `Authorization: Bearer <accessToken>`
* Expect:

  * HTTP **204**
  * allow-list key removed
  * blacklist key exists with TTL until token expiration

### 9.2 Logout without Authorization header

* Request is rejected by Spring Security
* Expect:

  * HTTP **401 Unauthorized**
  * message: `"Invalid token"`

### 9.3 Logout with already expired token

* Provide expired token in header
* Expect:

  * HTTP **204**
  * no Redis changes

### 9.4 Unauthorized logout (no valid auth)

* Call without valid token to a protected endpoint
* Expect:

  * HTTP **401 Unauthorized**
  * message: `"Invalid token"`

---

End of Logout Flow

---

