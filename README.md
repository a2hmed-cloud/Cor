# CREOVA

**Everything Creators Need. One Creative Universe.**

CREOVA is an Android-first creative ecosystem built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Google Firebase** for video editors, graphic designers, photographers, content creators, and digital artists.

## Architecture & Tech Stack

- **UI & Design System**: Jetpack Compose + Material 3 with an **Adaptive Liquid Glass** aesthetic (`Luminous Violet` + `Electric Cyan` accents on Deep Graphite dark surfaces and Warm Ivory light surfaces), supporting **System / Dark / Light** themes and **Reduced Motion**.
- **Bilingual RTL/LTR**: Arabic (RTL) default with instant in-app English (LTR) switcher, bundled **Cairo**, **IBM Plex Sans Arabic**, and **Plus Jakarta Sans** typography.
- **Persistence & Preferences**:
  - **Google Firebase Firestore** (named Enterprise database configured via `R.string.firestore_database_id`) for user profiles, creative tools, resources, community posts, comments/replies, saves, likes, follows, notifications, reports, and anti-spam engagement events.
  - **Jetpack DataStore** (`CreovaPreferences`) for local language, theme mode, Gemini model choice, reduced motion toggle, and recent searches.
- **Authentication**: **Google Sign-In via Jetpack Credential Manager** (`GetGoogleIdOption` silent auto sign-in + `GetSignInWithGoogleOption` interactive sheet) paired with Firebase Auth. Guests can browse the Tools Directory, Resource Library, and Community freely, and sign in with Google when interacting.
- **AI Assistant (CREOVA AI)**: Powered by **Firebase AI Logic (`Firebase.ai`)** with **Firebase App Check** (no embedded API keys). Supports configurable Gemini models (`gemini-3.8-flash`, `gemini-3.5-flash-lite`, `gemini-4-argon`), streaming chat, and `Schema.obj` structured JSON picks grounded against verified CREOVA catalog URLs.

## Key Modules

- `core/i18n/AppStrings.kt` — Bilingual Arabic/English string tables.
- `core/security/` — `AppCheckInitializer.kt`, `SafeUrlLauncher.kt` (HTTPS-only + executable blocker + Custom Tabs), `FirestoreErrorHandler.kt`.
- `core/media/ImageCompressor.kt` — Zero-permission Android Photo Picker (`PickVisualMedia`) compression pipeline.
- `domain/analytics/TrendingEngine.kt` — Transparent time-windowed scoring engine (`Today`, `This Week`, `This Month`) with deterministic per-user/day anti-spam IDs and `InsufficientData` protection.
- `data/catalog/CuratedCatalog.kt` — 19 verified tools across 6 creative categories and curated resources across all 19 library categories.
- `data/repository/` — `AuthRepository.kt`, `CreovaFirestoreRepository.kt`, `CreovaAiRepository.kt`.
- `ui/` — `CreovaTheme`, `LiquidGlass` components, `CreovaNavHost` (adaptive bottom bar / navigation rail), and all feature screens.

## Build & Verification

```bash
# Run JVM & Robolectric tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug

# Run Firestore Security Rules tests (requires local Firestore emulator on 127.0.0.1:8085)
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
