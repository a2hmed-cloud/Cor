# CREOVA Security Review

## 1. Zero-Trust Backend & Firestore Rules (`firestore.rules`)

- **Named Database Isolation**: All reads and writes target the provisioned Firestore Enterprise database (`R.string.firestore_database_id`), avoiding unprovisioned `(default)` access.
- **Fail-Closed Default**: Every collection explicitly declares `allow read, write` rules; unmatched paths are denied.
- **Strict Ownership & Field Validation**:
  - `users/{uid}`: Private settings/email strictly readable and writable only by `request.auth.uid == uid`; `accountStatus` is server-managed (`"active"` on create).
  - `public_profiles/{uid}`: Readable publicly; writable only by owner with non-spoofable counter defaults (`followersCount == 0`, etc.) on creation and immutable counters on client update.
  - `tools/{toolId}`: Publicly readable; writable only by verified admins (`/admins/{uid}`).
  - `resources/{resourceId}`: Approved items are publicly readable; authenticated creators can submit pending items with `https://` URLs; executable file extensions (`.apk`, `.exe`, `.dex`, `.jar`, `.so`, `.bat`, `.sh`) are rejected.
  - `posts/{postId}` & `comments`: Authenticated creation only with `authorId == request.auth.uid`, length caps, bounded base64 JPEG payload (`<= 350,000` chars), and safe `https://` linked URLs.
  - `engagement_events/{eventId}`: Deterministic per-user/per-day document IDs prevent synthetic vote inflation.
  - `reports/{reportId}`: Reporters can create open reports; only `/admins/{uid}` can read/resolve/dismiss reports.
- **Verified**: All 5 security rules test suites in `firestore.test.js` pass against the Firestore emulator.

## 2. Authentication & Session Security

- **Google-Only Auth**: Uses Jetpack `CredentialManager` (`GetGoogleIdOption` / `GetSignInWithGoogleOption`) + `FirebaseAuth` (`GoogleAuthProvider`). Anonymous auth (`signInAnonymously()`) and email/password fallbacks are absent.
- **Account Deletion**: Signed-in users can permanently delete their saved items, private user document, public profile, and Firebase Auth account from Settings.

## 3. Firebase AI Logic & App Check

- **Zero Embedded API Keys**: `CreovaAiRepository` uses `Firebase.ai.generativeModel(...)` authenticated via Firebase App Check (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds). No `GEMINI_API_KEY` is packaged in the APK.
- **Catalog URL Grounding**: `CreovaAiRepository.parseStructuredResponse` checks every AI-returned `verifiedUrl` against the verified `CuratedCatalog` allowlist, stripping any unverified or hallucinated URLs.

## 4. Client Link & Media Hardening

- **SafeUrlLauncher**: Rejects non-`https://` schemes (`http`, `javascript`, `file`, `intent`) and binary executable extensions (`.apk`, `.exe`, `.bat`, `.cmd`, `.msi`, `.dex`, `.jar`, `.sh`, `.ps1`, `.so`) before launching Android Custom Tabs.
- **Zero-Permission Photo Picker**: Post images use `ActivityResultContracts.PickVisualMedia` (`ImageOnly`), avoiding `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` permissions.
