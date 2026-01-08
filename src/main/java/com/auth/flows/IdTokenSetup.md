# How to Get Google `id_token` (Step by Step)

This guide shows the simple steps to get an **id_token**  
using **OAuth Playground**, and then use it in Postman.

---

## 1. Open OAuth Playground

1. Open your browser.
2. Go to: https://developers.google.com/oauthplayground

---

## 2. Add Your Client ID and Client Secret

1. On the top-right, click the **gear icon (Settings)**.
2. Check the box **Use your own OAuth credentials**.
3. Fill:

   - **OAuth Client ID**: your client ID  
   - **OAuth Client Secret**: your client secret

4. Click **Close**.

- Note: Authorized redirect URI:  
https://developers.google.com/oauthplayground
---

## 3. Select Scopes

1. On the left side, go to **Step 1**.
2. In the box **Input your own scopes**, type:

```

openid email

````

3. Click the blue button **Authorize APIs**.
4. A Google login window opens.
5. Log in with your Google account.
6. Click **Allow**.

Now you return to the Playground.

---

## 4. Exchange Code for Tokens

1. On the left, click **Step 2**.
2. You see an **Authorization code**.
3. Click the blue button:

**Exchange authorization code for tokens**

4. On the right side, you see a JSON response like:

```json
{
  "access_token": "...",
  "id_token": "eyJhbGciOi...",
  "expires_in": 3599,
  "token_type": "Bearer"
}
````

5. Copy the full value of **`id_token`**
   (this is a long string starting with `eyJ...`).

This is the token you will send to your backend.

---

## 5. Call `/auth/google` in Postman

1. Open Postman.

2. Make a new request:

    * Method: **POST**
    * URL: `http://localhost:8081/auth/google`

3. In **Headers**:

    * Set `Content-Type: application/json`
    * Make sure there is **no** `Authorization` header.

4. In **Body** → **raw** → **JSON**, write:

   ```json
   {
     "idToken": "PASTE_YOUR_ID_TOKEN_HERE"
   }
   ```

5. Click **Send**.

If all is correct, you get a response like:

```json
{
  "tokenType": "Bearer",
  "accessToken": "...",
  "refreshToken": "...",
  "username": "your_email@gmail.com"
}
```

---

## 6. Test `/auth/me` (Optional)

1. Copy the `accessToken`.

2. Make a new request:

    * Method: **GET**
    * URL: `http://localhost:8081/auth/me`

3. Add header:

   ```
   Authorization: Bearer YOUR_ACCESS_TOKEN
   ```

4. Click **Send**.

You will see your user info.

---

## 7. Done

Now you know how to create a new **id_token**
any time you need it for testing Google Login.

---