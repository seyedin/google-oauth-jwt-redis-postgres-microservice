# ==========================================
# Access Token Validation & Revocation Flow
# (JwtAuthFilter + Redis Allow/Blacklist)
# ==========================================

This document describes the **exact JWT validation flow** as implemented in the project,
based strictly on the provided PDF and existing source code.

---

## JWT Validation Flow – Table of Contents

0. Preconditions  
1. Filter registration & execution order  
2. Extract JWT from request  
3. Blacklist check (Redis)  
4. Allow-list check (Redis)  
5. Extract username from token  
6. Load user details (if needed)  
7. Validate JWT against user details  
8. Build Authentication object  
9. SecurityContext population  
10. Continue filter chain  
11. Error handling behavior (where 401 really happens)  
12. Test flow  

---

## 0. Preconditions

The following components already exist and are reused:

- `JwtAuthFilter` (`OncePerRequestFilter`)
- `JwtTokenProvider` (JWT parsing/validation utilities)
- `UserDetailsService`
- Redis allow-list service: `TokenAllowListService`
- Redis blacklist service: `TokenBlacklistService`
- `JwtAuthEntryPoint`
- Stateless Spring Security configuration

Redis keys:
- allow-list: `auth:allowlist:<token>`
- blacklist: `auth:blacklist:<token>`

---

## 1. Filter registration & execution order

- `JwtAuthFilter` is registered in the Spring Security filter chain.
- It executes **before** `UsernamePasswordAuthenticationFilter`.
- It runs for incoming requests and only short-circuits when:
  - `Authorization` header is missing, or
  - it does not start with `Bearer `

Note:
- It is **not skipped automatically** just because an endpoint is public.
- Public endpoints are permitted by `SecurityConfig`, not by skipping the filter.

---

## 2. Extract JWT from request

Steps:

1. Read `Authorization` header.
2. If header is missing or does not start with `"Bearer "`:
   - Do **not** authenticate.
   - Continue the filter chain.
3. If present:
   - Extract token via `substring(7)`.

---

## 3. Blacklist check (Redis)

- Check if the extracted token exists in Redis blacklist:
  - Key: `auth:blacklist:<token>`
- If the key exists:
  - Clear security context (`SecurityContextHolder.clearContext()`).
  - Continue the filter chain **without** setting authentication.
  - Return.

Important:
- The filter itself does **not** send a 401 response here.

---

## 4. Allow-list check (Redis)

- Check if the token exists in Redis allow-list:
  - Key: `auth:allowlist:<token>`
- If the key does **not** exist:
  - Clear security context (`SecurityContextHolder.clearContext()`).
  - Continue the filter chain **without** setting authentication.
  - Return.

Important:
- The filter itself does **not** send a 401 response here.

---

## 5. Extract username from token

- Extract username (subject) from the token using `JwtTokenProvider`.
- If token parsing fails (invalid signature, malformed token, expired token, etc.):
  - An exception is caught.
  - Clear security context.
  - Continue the filter chain without authentication.

---

## 6. Load user details (if needed)

- If username is present and there is no authentication in the context:
  - Load user details:
    - `userDetailsService.loadUserByUsername(username)`

---

## 7. Validate JWT against user details

- Validate token with `JwtTokenProvider.isValid(token, userDetails)` (or equivalent).
- If invalid:
  - Clear security context.
  - Continue the filter chain without authentication.

---

## 8. Build Authentication object

If token is valid:

- Create `UsernamePasswordAuthenticationToken` with:
  - principal = `UserDetails`
  - credentials = `null`
  - authorities = `userDetails.getAuthorities()`

---

## 9. SecurityContext population

- Set authentication:
  - `SecurityContextHolder.getContext().setAuthentication(authentication)`

User is now authenticated for the remainder of this request lifecycle.

---

## 10. Continue filter chain

- Always proceed with:
  - `filterChain.doFilter(request, response)`

---

## 11. Error handling behavior (where 401 really happens)

Key behavior:

- `JwtAuthFilter` **does not** produce a 401 response directly.
- It either:
  - sets authentication (when token is valid), or
  - clears context and continues (when token is missing/invalid/revoked/not allowed)

Where 401 is produced:

- If the endpoint is protected and, by the time authorization is evaluated,
  there is **no authenticated user**, Spring Security triggers:
  - `JwtAuthEntryPoint` → **401 Unauthorized**
  - message: `"Invalid token"`

---

## 12. Test Flow

### 12.1 Valid token
- Provide JWT in `Authorization: Bearer <token>`
- Token exists in allow-list
- Token not in blacklist
- Token is valid (signature + not expired)
- Expect:
  - authentication is set in SecurityContext
  - protected endpoint request succeeds

### 12.2 Missing Authorization header
- No token
- Expect:
  - public endpoints → allowed (no auth required)
  - protected endpoints → **401 Unauthorized** (triggered by SecurityConfig + JwtAuthEntryPoint)

### 12.3 Token revoked (blacklisted)
- Token exists in blacklist
- Expect:
  - filter clears context and continues
  - protected endpoints → **401 Unauthorized**
  - public endpoints → still accessible (no auth required)

### 12.4 Token not in allow-list
- Token valid but allow-list key missing
- Expect:
  - filter clears context and continues
  - protected endpoints → **401 Unauthorized**

### 12.5 Expired token
- Token expired
- Expect:
  - parsing/validation fails, filter clears context and continues
  - protected endpoints → **401 Unauthorized**

### 12.6 Invalid signature / malformed token
- Token signed with wrong secret or malformed
- Expect:
  - parsing/validation fails, filter clears context and continues
  - protected endpoints → **401 Unauthorized**

---

End of Access Token Validation & Revocation Flow


---