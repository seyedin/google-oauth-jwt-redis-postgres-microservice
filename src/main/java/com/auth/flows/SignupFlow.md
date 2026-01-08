# ==========================================
# Signup Implementation Flow (auth)
# ==========================================

This document describes the **exact signup flow** as implemented in the project,
based strictly on the provided PDF and existing source code.

---

## Signup Flow – Table of Contents

0. Create project and dependencies  
1. Configure `application.yml`  
2. Define `Role` enum  
3. Define `User` entity  
4. Define `UserRepository`  
5. Password hashing (`PasswordEncoder`)  
6. JWT generator (`JwtTokenProvider`)  
7. Refresh token model + repository  
8. Redis allow-list service  
9. AuthService.signup (business logic)  
10. Controller endpoint (`/auth/signup`)  
11. GlobalExceptionHandler  
12. Test flow  

---

## 0. Create project and dependencies

Required dependencies:

- spring-boot-starter-web  
- spring-boot-starter-security  
- spring-boot-starter-data-jpa  
- spring-boot-starter-validation  
- postgresql  
- jjwt-api  
- jjwt-impl  
- jjwt-jackson  
- spring-cloud-starter-netflix-eureka-client  
- springdoc-openapi-starter-webmvc-ui  
- spring-boot-starter-data-redis  
- lombok  

Test dependencies:

- spring-boot-starter-test  
- spring-security-test  

---

## 1. Configure `application.yml`

Signup requires the following configuration:

### Database
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

### JPA
- `spring.jpa.hibernate.ddl-auto`
- `spring.jpa.show-sql`
- `spring.jpa.properties.hibernate.dialect`

### JWT
- `jwt.secret`
- `jwt.expiration` (milliseconds)

### Redis
- Note: In the current implementation, Redis connection parameters
  are **hardcoded in `RedisConfig`**:
  - host = `localhost`
  - port = `6379`
  - password = `auth_pass`
- Therefore, `spring.data.redis.*` values in `application.yml`
  are **not used yet** by the application.

### Eureka
- `eureka.client.service-url.defaultZone`

---

## 2. Define `Role` enum

```java
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
````

---

## 3. Define `User` entity

Entity characteristics:

* Table name: `users`
* Implements `UserDetails`

### Fields

* `id`
* `username`
* `password`
* `email`
* `provider`
* `providerId`
* `role`
* `createdAt`
* `updatedAt`

### Validations

* `@NotBlank` on username and password
* `@Size(min = 8)` on password
* `@Email` on email
* `@NotNull` on role

### Lifecycle defaults (`@PrePersist`)

* If role is null → `ROLE_USER`
* If provider is null → `LOCAL`

---

## 4. Define `UserRepository`

Required methods:

* `boolean existsByUsername(String username)`
* `Optional<User> findByUsername(String username)`

(Additional method exists for Google flow but is not part of signup.)

---

## 5. Password hashing (`PasswordEncoder`)

* Defined in `SecurityConfig`
* Uses `BCryptPasswordEncoder`
* Password is always encoded **before saving user**

---

## 6. JWT generator (`JwtTokenProvider`)

Signup generates **access token** with:

* Subject: `username`
* Claim: `role`
* Expiration: `jwt.expiration`
* Signing algorithm: HS256
* Secret key derived from `jwt.secret`

---

## 7. Refresh token model + repository

### RefreshToken entity

* Table: `refresh_tokens`

Fields:

* `token`
* `user`
* `expiryDate`
* `revoked`
* `createdAt`

### RefreshTokenRepository

* `Optional<RefreshToken> findByToken(String token)`

Signup creates a refresh token with:

* Random UUID value
* Expiry: **30 days**
* `revoked = false`

---

## 8. Redis allow-list service

Signup stores **access token** in Redis allow-list.

- Key format: `auth:allowlist:<accessToken>`
- Value: `username`
- TTL: same as JWT expiration (`jwt.expiration`)

Purpose:
- Ensure stateless JWT can still be revoked
- Used by `JwtAuthFilter` in request validation

Implementation note:
- Redis connection settings are currently hardcoded in `RedisConfig`
  and not loaded from `application.yml`.

---

## 9. AuthService.signup (business logic)

Method: `signup(String username, String password, boolean isAdmin)`

### Steps

1. Validate username:

  * not null / not blank
  * length between **3 and 100**
2. Validate password:

  * not null / not blank
  * length **>= 8**
3. Check uniqueness:

  * if `existsByUsername(username)` → throw `IllegalStateException`
4. Create `User`:

  * encode password
  * role = `ROLE_USER`
  * provider = `LOCAL`
5. Save user in database
6. Generate access token (JWT)
7. Create refresh token (DB)
8. Store access token in Redis allow-list
9. Return `AuthResponseDto`

Returned DTO:

```
AuthResponseDto(
  type = "Bearer",
  accessToken,
  refreshToken,
  username
)
```

---

## 10. Controller endpoint (`/auth/signup`)

* Endpoint: `POST /auth/signup`
* Request body: `SignupRequestDto`
* Uses `@Valid` for DTO validation
* Calls `authService.signup(...)`
* Returns:

  * **201 Created**
  * Body: `AuthResponseDto`

---

## 11. GlobalExceptionHandler

Handled errors during signup:

### Validation errors

* Exception: `MethodArgumentNotValidException`
* Response: **400 Bad Request**
* Message: joined field validation messages

### Username already exists

* Exception: `IllegalStateException`
* Response: **409 Conflict**

Note:

* `IllegalArgumentException` from service-level validation
  is **not explicitly handled** and may result in default error response.

---

## 12. Test Flow

### 12.1 Successful signup

* Valid username and password
* Expect:

  * HTTP **201**
  * accessToken present
  * refreshToken present
  * Redis allow-list key exists

### 12.2 Username already exists

* Expect:

  * HTTP **409 Conflict**

### 12.3 Validation error

* Blank or invalid input
* Expect:

  * HTTP **400 Bad Request**

---

End of Signup Flow


