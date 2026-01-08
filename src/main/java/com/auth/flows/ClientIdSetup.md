# How to Create Google Client ID (Step by Step)

This guide shows simple steps to create a Google   
**Client ID** and put it in `application.yml`.

---

## 1. Open Google Cloud Console

1. Open your browser.
2. Go to: https://console.cloud.google.com
3. Log in with your Google account (Gmail).

---

## 2. Create or Select a Project

1. At the top bar, click on the **Project** selector (it may say “Select a project”).
2. To make a new project:
   - Click **New Project** or **Create Project**.
   - Type a project name, for example: `auth`.
   - Click **Create**.
3. Wait a few seconds.
4. Click the project selector again.
5. Select your new project.  
   Now this project is **active**.

---

## 3. Set Up OAuth Consent Screen

You do this only one time for the project.

1. On the left menu, click **APIs & Services**.
2. Click **OAuth consent screen**.
3. Choose **External**.
4. Click **Create** or **Continue**.
5. Fill the form:
   - **App name**: for example `Auth Local`.
   - **User support email**: your email.
   - **Developer contact information**: your email.
6. Click **Save and Continue**.
7. For scopes, you can just click **Save and Continue** (default is OK for simple tests).
8. For test users, you can add your email (optional) or click **Save and Continue**.
9. At the end, click **Back to Dashboard** or **Save**.

---

## 4. Open Credentials Page

1. On the left menu, click **APIs & Services**.
2. Click **Credentials**.

---

## 5. Create OAuth Client ID (Web Application)

1. On the top of the **Credentials** page, click **+ CREATE CREDENTIALS**.
2. Click **OAuth client ID**.
3. If Google asks again for the consent screen, complete it and save,  
   then come back to this step.
4. For **Application type**, choose **Web application**.
5. For **Name**, type something like: `auth-web-client`.
6. (Optional) Set **Authorized JavaScript origins**.
   - Example for local frontend: `http://localhost:3000`
7. Set **Authorized redirect URIs** (required for OAuth Playground):  
https://developers.google.com/oauthplayground
8. Click **Create**.
9. A small window opens. It shows:
   - **Client ID**
   - **Client secret**
10. Copy the **Client ID**.  
    It looks like:  
    `1234567890-abcde12345fghij.apps.googleusercontent.com`

---

## 6. Put Client ID in `application.yml`

1. Open your Spring Boot project.
2. Open `src/main/resources/application.yml`.
3. Find this block:

```yaml
   google:
     client-id: YOUR_GOOGLE_CLIENT_ID_HERE
```
4. Replace the value with your real Client ID:

```yaml
   google:
     client-id: 1234567890-abcde12345fghij.apps.googleusercontent.com
   ```

5. Save the file.

Note:
- This project uses ONLY the Google Client ID.
- The Client Secret is NOT used or required.
- The backend validates the `aud` claim in the `id_token`
  against `google.client-id`.

---

## 7. Restart Spring Boot Application

1. Stop your Spring Boot app if it is running.
2. Run the app again.
3. Now your backend uses the new Google **Client ID**.

---

## 8. Quick Check

- If you call `/auth/google` with an `id_token`
  that was created for the **same Google Client ID**
  configured in your backend, the request will be accepted.

- If you see the following error message:

Invalid Google token audience
- it means the `id_token` was created for a **DIFFERENT**
Google Client ID than the one configured in:

google.client-id
- To fix this:
- Make sure the `id_token` is generated using the **same Client ID**
  that is set in `application.yml`.
- Re-generate the `id_token` after updating the Client ID.
- Restart the Spring Boot application if the configuration was changed.---