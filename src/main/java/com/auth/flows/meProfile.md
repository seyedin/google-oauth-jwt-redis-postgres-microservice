# ==========================================
# Current User Profile Flow (auth/me)
# ==========================================

This document describes the **exact /auth/me flow** as implemented in the project,
based strictly on the provided PDF and existing source code.

---

## /auth/me Flow – Table of Contents

0. Preconditions  
1. Controller endpoint (`/auth/me`)  
2. Authentication requirement (`@AuthenticationPrincipal`)  
3. JwtAuthFilter responsibility (how user is set)  
4. AuthService.me (business logic)  
5. Response DTO (`UserProfileDto`)  
6. Error handling  
7. Test flow  

---

## 0. Preconditions

The following components already exist and are reused:

- Protected endpoints require authentication (SecurityConfig)
- `JwtAuthFilter` sets authentication when access token is valid and allowed
  (JWT validation + Redis allow-list/blacklist enforcement)
- `User` implements `UserDetails`
- `AuthService.me(User user)` returns profile data
- DTO: `UserProfileDto(id, username, role)`

---

## 1. Controller endpoint (`/auth/me`)

- Endpoint: `GET /auth/me`
- Authentication: **required** (not in permitAll list)
- Reads current user via `@AuthenticationPrincipal`
- Delegates to `authService.me(user)`
- Returns **200 OK** with `UserProfileDto`

Controller method shape:
- `me(@AuthenticationPrincipal User user)`

---

## 2. Authentication requirement (`@AuthenticationPrincipal`)

- Endpoint is protected by Spring Security.
- If request is **unauthenticated** (missing/invalid token or no authentication set),
  the controller is **not executed** and Spring Security triggers **401 Unauthorized**
  via `JwtAuthEntryPoint`.
- If authenticated, Spring injects the current `User` into the controller parameter.

Important implementation detail:
- Neither the controller nor `AuthService.me(...)` performs a null-check on `user`.
  The method assumes an authenticated request (otherwise a `NullPointerException`
  would occur, but in practice the request is blocked earlier by SecurityConfig).

---

## 3. JwtAuthFilter responsibility (how user is set)

Before the controller runs:

1. `JwtAuthFilter` reads `Authorization: Bearer <token>`
2. Checks token is NOT blacklisted (Redis)
3. Checks token exists in allow-list (Redis)
4. Extracts username from token
5. Loads user by username
6. Validates token against loaded user details
7. Sets `SecurityContextHolder` authentication

After this, `@AuthenticationPrincipal` resolves to the authenticated `User`.

---

## 4. AuthService.me (business logic)

Method:
```java
UserProfileDto me(User user)
````

Behavior:

* Builds and returns `UserProfileDto` from the current user:

    * `id = user.getId()`
    * `username = user.getUsername()`
    * `role = user.getRole().name()`

No database lookup is performed in this method.

---

## 5. Response DTO (`UserProfileDto`)

Returned DTO:

```java
UserProfileDto(
  Long id,
  String username,
  String role
)
```

Controller response:

* HTTP **200 OK**
* Body: `UserProfileDto`

---

## 6. Error handling

### 6.1 Unauthorized (missing/invalid token)

If token is missing OR invalid OR revoked OR not in allow-list:

* `JwtAuthFilter` does not set authentication
* Spring Security blocks the protected endpoint
* `JwtAuthEntryPoint` returns:

    * **401 Unauthorized**
    * message: `"Invalid token"`

### 6.2 Other runtime errors

* Not explicitly handled for this endpoint (default Spring Boot behavior).

---

## 7. Test Flow

### 7.1 Successful request

* Call `GET /auth/me` with valid `Authorization: Bearer <accessToken>`
* Token is in allow-list and not in blacklist
* Expect:

    * HTTP **200**
    * response contains:

        * `id`
        * `username`
        * `role`

### 7.2 Unauthorized (no token)

* Call without `Authorization` header
* Expect:

    * HTTP **401 Unauthorized**
    * message: `"Invalid token"`

### 7.3 Unauthorized (blacklisted token)

* Call with a token in blacklist
* Expect:

    * HTTP **401 Unauthorized**
    * message: `"Invalid token"`

### 7.4 Unauthorized (not in allow-list)

* Call with token not in allow-list
* Expect:

    * HTTP **401 Unauthorized**
    * message: `"Invalid token"`

---

End of /auth/me Flow

---
