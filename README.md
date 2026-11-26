# Digital Closet — prototype

The Android app remains an offline, in-memory **prototype**, not the connected MVP.
It has sample garments, local garment entry, and a deterministic example outfit.
Changes disappear when the app process is restarted. There are no accounts or
persisted photos yet, and the app is not connected to the new API.

## Android

Use JDK 17+, Android SDK 36, and Gradle 9.6.0. Run `gradle :composeApp:testAndroidHostTest
:androidApp:assembleDebug` (add `--offline` when all dependencies are cached).
The installable debug APK is `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
Android Studio can open this directory as a Gradle project. A debug APK is automatically
signed for testing; a release build requires its own signing key.

## iOS

The shared Compose module declares iOS device and simulator targets on macOS.
An Xcode host application and a macOS build/signing environment are still needed
to run an iOS app; this Windows checkout cannot verify iOS.

## API foundation

`contracts` holds shared, serializable request and response DTOs. `server` is a
Ktor/JVM API. From a local machine with Gradle 9.6 and a JDK 17+ installation,
run `gradle :contracts:serverTest :contracts:testAndroidHostTest :server:test`
to check contracts on JVM and Android plus the HTTP routes. The API listens on
`127.0.0.1:8080` (or the configured `PORT`). Both `GET /health` and
`GET /api/v1/health` return `{"status":"ok"}`. An invalid `PORT` fails at
startup instead of silently using 8080. This is not a deployed API.

### Local API configuration

Set `JWT_SECRET` to a fresh, unpadded base64url encoding of at least 32 random
bytes. For example, generate a key with
`openssl rand -base64 32 | tr '+/' '-_' | tr -d '='`. Export the values in your
shell before starting the API. `.env.example` lists the supported variables;
the server does not load `.env` files itself.
`JWT_SECRET` is required and a missing, malformed, or undersized value stops
startup before the server listens. `JWT_ISSUER` defaults to `digital-closet`,
`JWT_AUDIENCE` defaults to `digital-closet-client`, and `DATABASE_PATH` defaults
to `data/digital-closet.sqlite`; set the database path to a writable
server-side SQLite file. The initial versioned schema migration runs before the
server accepts requests.

Run `./gradlew --offline :server:run` after exporting those variables (omit
`--offline` only when approved public dependencies are not cached). Gradle
resolves dependencies from the public Google Maven, Maven Central, and Plugin
Portal repositories; no GitHub or Maven credentials are required.

### Authentication and profile API

- `POST /api/v1/auth/register` and `POST /api/v1/auth/login` accept the shared
  `RegisterRequest` and `LoginRequest` DTOs and return `SessionResponse`.
- `POST /api/v1/auth/refresh` accepts `RefreshRequest`; each successful refresh
  rotates its random refresh token, and replaying a replaced token is denied.
- `DELETE /api/v1/auth/session` accepts the current refresh token in
  `RefreshRequest` and revokes it. It intentionally returns `204` even when
  the supplied token is already invalid.
- `GET /api/v1/profile` and `PUT /api/v1/profile` require a bearer access token.
  The profile owner comes from the verified JWT subject; request bodies cannot
  select another account. Unknown JSON fields (including `ownerId`) and malformed
  request bodies are rejected with a `400` `ApiError` response.

Passwords are stored as per-user PBKDF2-HMAC-SHA256 hashes. SQLite stores only
refresh-token hashes; access JWTs are signed and expire after 15 minutes, while
refresh tokens expire after 30 days. Keep the database on the server: SQLite
is not part of the mobile client. These routes are local MVP functionality,
not a deployed API.

## Next milestone

Continue with private image storage and real garment registration. Do not treat
demo state as durable.
