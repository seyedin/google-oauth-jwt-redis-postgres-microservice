# ==========================================
# Architecture Diagram – Auth Service
# ==========================================

This document describes the **high-level architecture** of the Auth Service,
based strictly on the provided project PDF and existing source code.

---

## 1. High-Level Architecture Overview

The Auth Service is a **stateless Spring Boot microservice** responsible for:

- User authentication (local & Google)
- JWT access token generation and validation
- Refresh token management
- Token revocation using Redis
- User profile retrieval
- Eureka service registration

---

## 2. Main Components

```

┌──────────────┐
│   Client     │
│ (Browser /   │
│  Mobile /    │
│  Postman)    │
└──────┬───────┘
│ HTTP (REST)
▼
┌──────────────────────────┐
│     Auth Controller      │
│  (/auth/* endpoints)     │
└──────┬───────────────────┘
│ delegates
▼
┌──────────────────────────┐
│        Auth Service      │
│  (Business Logic Layer)  │
└──────┬───────────────────┘
│
├──────────────┬───────────────┬───────────────┐
▼              ▼               ▼               ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ PostgreSQL  │ │   Redis     │ │ Google APIs │ │ JWT Provider│
│ (Users &    │ │ (Allow /    │ │ (id_token   │ │ (Token Gen  │
│ RefreshTok) │ │ Blacklist)  │ │ verification)│ │ & Validate)│
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘

```

---

## 3. Request Flow (Authenticated Requests)

```

Client
│
│ Authorization: Bearer <accessToken>
▼
JwtAuthFilter
│
├─ Check Redis Blacklist
├─ Check Redis Allow-list
├─ Validate JWT (signature + subject)
├─ Load User from DB
▼
SecurityContextHolder
│
▼
Controller → Service → Response

```

---

## 4. Authentication Flows

### 4.1 Local Login / Signup

```

Client
│
│ POST /auth/login | /auth/signup
▼
AuthController
▼
AuthService
├─ AuthenticationManager (login)
├─ PasswordEncoder (signup)
├─ JWT generation
├─ Refresh token creation (DB)
├─ Add access token to Redis allow-list
▼
Response (AuthResponseDto)

```

---

### 4.2 Google Login

```

Client
│
│ POST /auth/google (idToken)
▼
AuthController
▼
AuthService.loginWithGoogle
├─ GoogleAuthService.verifyIdToken
│     └─ Google tokeninfo endpoint
├─ Find or create GOOGLE user (PostgreSQL)
├─ Generate JWT + refresh token
├─ Add access token to Redis allow-list
▼
Response (AuthResponseDto)

```

---

### 4.3 Refresh Token

```

Client
│
│ POST /auth/refresh (refreshToken)
▼
AuthController
▼
AuthService.refreshToken
├─ Load refresh token (DB)
├─ Validate (exists, not revoked, not expired)
├─ Revoke old refresh token
├─ Generate new access + refresh tokens
├─ Add new access token to Redis allow-list
▼
Response (AuthResponseDto)

```

---

### 4.4 Logout

```

Client
│
│ POST /auth/logout
▼
AuthController
▼
AuthService.logout
├─ Remove access token from Redis allow-list
├─ Add access token to Redis blacklist (TTL until expiration)
▼
204 No Content

```

---

### 4.5 Current User (`/auth/me`)

```

Client
│
│ GET /auth/me
▼
JwtAuthFilter
├─ Token validation
├─ Redis allow/blacklist checks
├─ Set SecurityContext
▼
AuthController
▼
AuthService.me
▼
UserProfileDto

```

---

## 5. Data Stores

### PostgreSQL
- `users`
- `refresh_tokens`

### Redis
- Allow-list keys:  
  `auth:allowlist:<accessToken>`
- Blacklist keys:  
  `auth:blacklist:<accessToken>`

---

## 6. Security Characteristics

- Stateless authentication (no HTTP session)
- JWT access tokens
- Refresh tokens stored in DB (one-time use)
- Redis-based token revocation
- GlobalExceptionHandler for application errors
- JwtAuthEntryPoint for authentication/authorization errors
- Protected endpoints enforced by Spring Security

---

## 7. Service Discovery

```

Auth Service
│
│ registers
▼
Eureka Server

```

- Service name: `AUTH`
- Heartbeat & lease configuration via `application.yml`

---

End of Architecture Diagram

---
