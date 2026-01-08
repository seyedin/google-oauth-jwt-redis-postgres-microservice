# ==========================================
# Google Login Full Flow (Frontend + Backend)
# ==========================================

This document explains the **full Google Login flow**
from zero to working login.

You can follow this file step by step and finish the project.

---

## 1. Requirements

You need:

- Docker and Docker Compose
- Java + Maven (Backend)
- Node.js + npm (Frontend tooling only)
- Google account (Gmail)

---

## 2. Start Database and Redis (Docker)

In the project root (where `docker-compose.yml` exists):

```bash
docker compose up -d
````

Check services:

```bash
docker compose ps
```

Result:

* PostgreSQL is running
* Redis is running

---

## 3. Start Backend (Spring Boot Auth Service)

Go to the **auth** service folder.

Run with Maven:

```bash
mvn spring-boot:run
```

Or with jar:

```bash
java -jar target/auth-*.jar
```

Backend runs on:

```
http://localhost:8081
```

---

## 4. Create React App (Frontend)

Go to the folder where you want frontend:

```bash
npm create vite@latest auth-web -- --template react
cd auth-web
npm install
npm run dev
```

Frontend runs on:

```
http://localhost:5173
```

(or 5174 if port is busy)

---

## 5. Install Frontend Libraries

Inside `auth-web` folder:

```bash
npm install axios @react-oauth/google
```

---

## 6. Create Google Client ID (Web)

1. Open Google Cloud Console
   [https://console.cloud.google.com](https://console.cloud.google.com)

2. Create a project (or use existing)

3. Go to:

    * APIs & Services
    * OAuth consent screen
    * Choose **External**
    * Fill app name and email
    * Save

4. Go to:

    * APIs & Services
    * Credentials
    * Create credentials
    * OAuth Client ID
    * Application type: **Web application**

5. Set:

    * Name: auth-web-client
    * Authorized JavaScript origins:

      ```
      http://localhost:5173
      http://localhost:5174
      ```

6. Save and copy **Client ID**

Example:

```
1234567890-abcde.apps.googleusercontent.com
```

---

## 7. Add Google Client ID to React

### File: `src/main.jsx`

Wrap the app with `GoogleOAuthProvider`.

```jsx
import React from "react";
import ReactDOM from "react-dom/client";
import { GoogleOAuthProvider } from "@react-oauth/google";
import App from "./App.jsx";

const GOOGLE_CLIENT_ID = "YOUR_CLIENT_ID.apps.googleusercontent.com";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <App />
    </GoogleOAuthProvider>
  </React.StrictMode>
);
```

---

## 8. Create Google Login UI (React)

### File: `src/App.jsx`

```jsx
import { GoogleLogin } from "@react-oauth/google";
import axios from "axios";
import { useState } from "react";

const BACKEND_BASE_URL = "http://localhost:8081";

export default function App() {
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleGoogleSuccess = async (credentialResponse) => {
    try {
      setError(null);

      // Google gives idToken here
      const idToken = credentialResponse.credential;

      // Send idToken to backend
      const res = await axios.post(
        `${BACKEND_BASE_URL}/auth/google`,
        { idToken }
      );

      setResult(res.data);
    } catch (e) {
      setResult(null);
      setError(e?.response?.data ?? e.message);
    }
  };

  return (
    <div style={{ padding: 24 }}>
      <h1>Auth Web</h1>

      <GoogleLogin
        onSuccess={handleGoogleSuccess}
        onError={() => setError("Google Login Failed")}
      />

      {result && (
        <>
          <h3>Backend Response</h3>
          <pre>{JSON.stringify(result, null, 2)}</pre>
        </>
      )}

      {error && (
        <>
          <h3>Error</h3>
          <pre>{JSON.stringify(error, null, 2)}</pre>
        </>
      )}
    </div>
  );
}
```

---

## 9. Backend CORS Configuration (Important)

### File: `CorsConfig.java`

Location:

```
auth/src/main/java/com/auth/security/CorsConfig.java
```

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
```

---

## 10. Enable CORS in SecurityConfig

### File: `SecurityConfig.java`

Add `.cors()` to security filter chain.

```java
http
  .cors()
  .csrf(csrf -> csrf.disable())
```

---

## 11. Google Login Backend Flow

When user clicks **Sign in with Google**:

1. Google shows login page
2. User selects Google account
3. Google returns **idToken** to frontend
4. Frontend sends:

   ```
   POST /auth/google
   {
     "idToken": "..."
   }
   ```
5. Backend verifies idToken with Google
6. Backend finds or creates user
7. Backend creates:

    * Access Token (JWT)
    * Refresh Token
8. Backend returns tokens to frontend

---

## 12. Final Test

1. Backend is running on `8081`
2. Frontend is running on `5173`
3. Open browser:

   ```
   http://localhost:5173
   ```
4. Click **Sign in with Google**
5. Select Google account
6. You see response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "...",
  "refreshToken": "...",
  "username": "email@gmail.com"
}
```

✅ Google Login works.

---

End of Google Login Full Flow

---
