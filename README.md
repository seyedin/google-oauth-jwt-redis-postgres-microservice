# Google OAuth JWT Auth Microservice

This project is an **Authentication Microservice**.

It supports:

- Login with **Google**
- Login with **username and password**
- **JWT authentication**
- **Refresh token**
- **Redis allow-list and blacklist**
- **PostgreSQL database**
- **Docker and Docker Compose**

This service is designed to be used by other microservices.

---

## Architecture Overview

This project uses **microservice architecture**.

Main components:

- Auth Service (Spring Boot)
- PostgreSQL (User data)
- Redis (Token allow-list and blacklist)
- Google OAuth (Login with Google)
- Docker (Run everything together)

See details in:
- `flows/ArchitectureDiagram.md`

---

## Authentication Features

### 1. Signup (Email & Password)

- User sends signup request
- Password is encrypted
- User is saved in PostgreSQL
- JWT tokens are created

See flow:
- `flows/SignupFlow.md`

---

### 2. Login (Email & Password)

- User sends username and password
- Credentials are validated
- Access token and refresh token are returned

See flow:
- `flows/login-flow.md`

---

### 3. Login with Google (OAuth)

- Frontend gets `id_token` from Google
- Backend verifies `id_token`
- User is created if not exists
- JWT access token and refresh token are returned

See flows:
- `flows/GoogleLoginFlow.md`
- `flows/ClientIdSetup.md`
- `flows/IdTokenSetup.md`
- `flows/ReactFrontendSetupFlow.md`

---

### 4. JWT Security

- Access token is short-lived
- Refresh token is long-lived
- Every request uses Authorization header

See details:
- `flows/JWTAuth+RedisAllowBlacklist.md`

---

### 5. Refresh Token Flow

- Client sends refresh token
- Old access token is replaced
- New access token is generated

See flow:
- `flows/RefreshFlow.md`

---

### 6. Logout

- Access token is added to Redis blacklist
- Token is no longer valid

See flow:
- `flows/logout-flow.md`

---

### 7. User Profile (Me Endpoint)

- Client sends access token
- Backend returns user profile info

See flow:
- `flows/meProfile.md`

---

## Redis Usage

Redis is used for security:

- **Allow-list**: valid access tokens
- **Blacklist**: revoked tokens (logout)

This keeps the system **stateless** and secure.

---

## Database

PostgreSQL stores:

- Users
- Roles
- Refresh tokens

Database is initialized automatically with Docker.

---

## Configuration

There are two config files:

- `application.yaml` → local run
- `application-docker.yml` → Docker run


Important environment variables:

```env
JWT_SECRET=your_secret_key
GOOGLE_CLIENT_ID=your_google_client_id
```
---
## Run with Docker  
From project root:
```bash
docker compose up -d
```
Services:  
- Auth Service → http://localhost:8081
- PostgreSQL → port 5432
- Redis → port 6379

---
## API Documentation
Swagger UI is enabled.
After start:
``` bash
http://localhost:8081/swagger-ui.html
```
---
## Health Check

Health endpoint:
```bash
GET /actuator/health
```
---
## Project Status

This project is production-ready for authentication.

Next steps are listed in:  
- flows/TODO.md
---
## Summary

This project provides:  
- Secure authentication
- Google OAuth login
- JWT + Redis token control
- Clean microservice design
- Full documentation for each flow
- It can be used as the auth service for real systems.


