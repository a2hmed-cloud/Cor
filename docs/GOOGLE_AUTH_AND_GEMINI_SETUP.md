# Google Auth, Firestore & Gemini (Firebase AI Logic) Setup Guide

CREOVA uses **Google-only cloud infrastructure** (Firebase Auth with Google Sign-In, Cloud Firestore Enterprise, Firebase App Check, and Firebase AI Logic for Gemini). No third-party backend or Supabase is used.

## 1. Google Sign-In & Credential Manager

1. Ensure `app/google-services.json` is present in `app/` and includes your debug and release SHA-1/SHA-256 fingerprints in the Firebase Console (`Project Settings -> Your apps -> Android`).
2. `google-services.json` automatically generates `R.string.default_web_client_id`, which `AuthRepository` passes to `GetGoogleIdOption` (silent auto sign-in) and `GetSignInWithGoogleOption` (interactive Google account picker).
3. Verify **Google** is enabled under **Firebase Console -> Authentication -> Sign-in method**.

## 2. Cloud Firestore Named Database & Rules

1. The provisioned database ID is stored in `app/src/main/res/values/firebase_applet_config.xml` (`R.string.firestore_database_id`).
2. `MainActivity` always obtains the instance via:
   ```kotlin
   val databaseId = appContext.getString(R.string.firestore_database_id)
   val firestore = FirebaseFirestore.getInstance(databaseId)
   ```
3. Security rules are defined in `firestore.rules` and verified locally via `firestore.test.js`.
4. To grant admin moderation access to a Google user, create `/admins/{uid}` in the Firestore console with `{ "role": "admin" }`.

## 3. CREOVA AI (Firebase AI Logic + App Check)

1. CREOVA calls Gemini models through `Firebase.ai.generativeModel(...)` — **no `GEMINI_API_KEY` is needed in `.env` or APK code**.
2. `MainActivity.onCreate()` invokes `configureAppCheck(this, intent)` **before** `setContent` or ViewModel initialization:
   - Debug builds install `DebugAppCheckProviderFactory` (accepting `FIREBASE_APPCHECK_DEBUG_TOKEN` intent extra or printing a debug token to Logcat).
   - Release builds install `RecaptchaAppCheckProviderFactory` using `R.string.recaptcha_site_key` from `firebase_applet_config.xml`.
3. Users can switch between `gemini-3.8-flash` (default), `gemini-3.5-flash-lite`, and `gemini-4-argon` inside **Settings** or directly in the **CREOVA AI** screen.
