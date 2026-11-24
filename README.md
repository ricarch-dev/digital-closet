# Digital Closet — prototype

This is an offline, in-memory **prototype**, not the connected MVP. It has sample garments,
local garment entry, and a deterministic example outfit. Changes disappear when the app
process is restarted. There are no accounts, persisted photos, backend, or deployment.

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

## Next milestone

Build the Ktor API, private image storage, server-side SQLite persistence,
authentication, and real garment registration. Do not treat demo state as durable.
