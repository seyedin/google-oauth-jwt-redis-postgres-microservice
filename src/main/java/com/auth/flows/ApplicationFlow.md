# ==========================================
# Application Authentication Flow
# ==========================================

This document explains the **full authentication flow** of the application
in a simple and clear way, based on the actual project implementation.

---

## 1. Application Start

- The application starts with Spring Boot.
- The application creates a default admin user if it does not exist.
- The application connects to PostgreSQL.
- The application connects to Redis.
- The application runs in **stateless mode** (no HTTP session).

---

## 2. Security Configuration

- Some endpoints are **public**:
  - `/auth/login`
  - `/auth/signup`
  - `/auth/refresh`
  - `/auth/google`

- All other endpoints need a valid **Access Token**.
- The application uses `JwtAuthFilter` for every request.

---

## 3. JwtAuthFilter (Runs for Every Request)

- Read the `Authorization` header.
- If the header is not `Bearer <token>` → skip authentication.
- Check Redis **blacklist**:
  - If token exists → authentication fails.
- Check Redis **allow-list**:
  - If token does not exist → authentication fails.
- Validate JWT signature and expiration.
- Extract username from token.
- Load user from database.
- Set authentication in `SecurityContextHolder`.

---

## 4. User Model

- User fields:
  - id
  - username
  - password
  - role
  - provider
  - providerId
- Password is stored as a **hashed value**.
- User data is stored in **PostgreSQL**.

---

## 5. Refresh Token Model

- Token value (UUID)
- Related user
- Expiration date
- Revoked flag (true / false)
- Stored in **PostgreSQL**

---

## 6. Allow-List (Redis)

- Stores **active access tokens**.
- Token is added on:
  - signup
  - login
  - refresh
  - google login
- Token is removed on logout.

---

## 7. Blacklist (Redis)

- Stores **revoked access tokens**.
- Token is added on logout.
- Token stays until its TTL expires.

---

# Main Authentication Flows

---

## 8. Signup Flow

- User sends `username` and `password`.
- Application checks if username already exists.
- Application creates a new user.
- Application generates an Access Token.
- Application generates a Refresh Token (saved in DB).
- Application adds Access Token to Redis allow-list.
- Application returns:
  - tokenType
  - accessToken
  - refreshToken
  - username

---

## 9. Login Flow

- User sends `username` and `password`.
- Application validates credentials.
- Application generates new Access Token.
- Application generates new Refresh Token.
- Application adds Access Token to Redis allow-list.
- Application returns tokens to user.

---

## 10. Refresh Token Flow

- User sends a Refresh Token.
- Application checks:
  - token exists in database
  - token is not revoked
  - token is not expired
- Application revokes old Refresh Token.
- Application generates:
  - new Access Token
  - new Refresh Token
- Application adds new Access Token to allow-list.
- Application returns new tokens.

---

## 11. Logout Flow

- User sends request with Access Token.
- Application removes token from allow-list.
- Application adds token to blacklist.
- Application returns **HTTP 204 No Content**.

---

## 12. Protected Endpoint Flow (Example: `/auth/me`)

- User sends request with Access Token.
- JwtAuthFilter checks:
  - blacklist
  - allow-list
  - JWT validity
- If valid:
  - user is authenticated
  - controller returns data
- If invalid:
  - application returns **401 Unauthorized**

---

## 13. Token Life Cycle

### Access Token

- Short lifetime.
- Stored in Redis allow-list.
- Removed on logout.
- Added to blacklist after logout.

### Refresh Token

- Long lifetime.
- Stored in PostgreSQL.
- Used to create new Access Token.
- **Single-use only**.
- After use → token is revoked.

---

## 14. Summary

- Application is **stateless**.
- Redis manages Access Token state (allow-list + blacklist).
- PostgreSQL stores users and Refresh Tokens.
- Signup/Login → create tokens.
- Refresh → rotate tokens.
- Logout → revoke tokens.
- All protected endpoints need a valid Access Token.

---

## 15. Test Flow

### 15.1 Prerequisites

- Docker and Docker Compose installed
- `docker-compose.yml` exists with:
  - PostgreSQL
  - Redis
- Spring Boot application runs on port **8080**
- `init-db` folder exists for database initialization

---

### 15.2 Start Services

From project root:

```bash
docker compose up -d
````

Check services:

```bash
docker compose ps
```

---

### 15.3 Run Application

Using Maven:

```bash
mvn spring-boot:run
```

Or using JAR:

```bash
java -jar target/auth-*.jar
```

Expected log:

```
Started AuthApplication in X seconds
```

---

### 15.4 Test Signup (Local User)

Request:

```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "sanaz",
    "password": "MyStrongPass123"
  }'
```

Expected response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "...",
  "refreshToken": "...",
  "username": "sanaz"
}
```

---

### 15.5 Verify Database

PostgreSQL:

```sql
SELECT username, provider, provider_id, role
FROM users
WHERE username = 'sanaz';
```

Expected:

* provider = LOCAL
* provider_id = null
* role = ROLE_USER

---

### 15.6 Verify Redis Allow-List

```bash
docker compose exec redis redis-cli
keys auth:allowlist:*
```

* At least one key must exist.
* Value should be the username.

---

### 15.7 Test Login

* Call `/auth/login` with same credentials.
* Expect new tokens and new allow-list entry.

---

### 15.8 Test Refresh Token

* Call `/auth/refresh` with refresh token.
* Expect:

  * new access token
  * new refresh token
  * old refresh token revoked

---

### 15.9 Test Logout

* Call `/auth/logout` with access token.
* Expect:

  * HTTP 204
  * token removed from allow-list
  * token added to blacklist

---

### 15.10 Test Protected Endpoint

* Call `/auth/me` with:

  * valid token → **200 OK**
  * logged-out token → **401 Unauthorized**

---

End of Authentication Flow

---


