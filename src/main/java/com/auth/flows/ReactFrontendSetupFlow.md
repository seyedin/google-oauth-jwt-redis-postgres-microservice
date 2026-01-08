# ==========================================
# React Frontend Setup Flow (auth-web) - (Vite + React + Axios + Google OAuth)
# ==========================================

This document explains **how to create and run the React frontend**
for the Auth project.

---

## 1. Requirements

You need:

* **Node.js**
* **npm**
* Internet connection

Check Node.js:

```bash
node -v
npm -v
```

If command works, Node.js is ready.

---

## 2. Create React Project (Vite)

Go to folder where you want frontend:

```bash
npm create vite@latest auth-web -- --template react
```

Enter project folder:

```bash
cd auth-web
```

Install packages:

```bash
npm install
```

---

## 3. Run React App

Start frontend:

```bash
npm run dev
```

Frontend runs on:

```
http://localhost:5173
```

(5174 if port is busy)

---

## 4. Install Frontend Libraries

Inside `auth-web` folder run:

```bash
npm install axios @react-oauth/google
```

These libraries are used for:

* HTTP request (axios)
* Google Login (@react-oauth/google)

---

## 5. Create Environment File

Create file `.env` in project root:

```env
VITE_GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID
VITE_BACKEND_BASE_URL=http://localhost:8081
```

---

## 6. Setup Google Provider

### File: `src/main.jsx`

Wrap app with Google provider:

```jsx
import React from "react";
import ReactDOM from "react-dom/client";
import { GoogleOAuthProvider } from "@react-oauth/google";
import App from "./App.jsx";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <App />
    </GoogleOAuthProvider>
  </React.StrictMode>
);
```

---

## 7. Create Google Login UI

### File: `src/App.jsx`

```jsx
import { GoogleLogin } from "@react-oauth/google";
import axios from "axios";
import { useState } from "react";

const BACKEND_BASE_URL = import.meta.env.VITE_BACKEND_BASE_URL;

export default function App() {
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleGoogleSuccess = async (credentialResponse) => {
    try {
      setError(null);

      const idToken = credentialResponse.credential;

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

      {result && <pre>{JSON.stringify(result, null, 2)}</pre>}
      {error && <pre>{JSON.stringify(error, null, 2)}</pre>}
    </div>
  );
}
```

---

## 8. Connect to Backend

Make sure backend is running on:

```
http://localhost:8081
```

Frontend sends request to:

```
POST /auth/google
```

With body:

```json
{
  "idToken": "..."
}
```

---

## 9. Final Test

1. Backend is running
2. Frontend is running
3. Open browser:

   ```
   http://localhost:5173
   ```
4. Click **Sign in with Google**
5. You get JWT tokens from backend

✅ React frontend works.

---

