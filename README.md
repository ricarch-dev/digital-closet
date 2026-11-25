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
Ktor/JVM application with only a health route so far. From a local machine with
Gradle 9.6 and a JDK 17+ installation, run `gradle :contracts:serverTest
:server:test` to check the contracts and HTTP route; run `gradle :server:run`
to listen on `127.0.0.1:8080` (or set `PORT`). Both `GET /health` and
`GET /api/v1/health` return `{"status":"ok"}`. This is not a deployed API.

## Next milestone

Add authentication, private image storage, server-side SQLite persistence and
real garment registration. Do not treat demo state as durable.
