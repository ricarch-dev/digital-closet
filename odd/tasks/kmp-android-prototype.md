# Kotlin Multiplatform Android prototype
Branch: `feat/kmp-android-prototype`. Delivery: `ask-on-risk`; forecast: ~485 authored lines (excluding installed third-party skills). Runner: Gradle 9.6, Android SDK 36.
Objective: Create a locally buildable Android prototype with sample closet data and a simple outfit; no backend deployment or publishing.

## Specs
S1. "primero un APK de prototipo con datos de ejemplo, sin despliegue" — Android prototype can be built locally; no server, account, or remote storage is needed. Check: offline Android build if cached dependencies permit.
S2. "1. Prototipo | Navegación, clóset, alta de prenda y ejemplo de outfit | Que la idea se entiende antes de construir todo el sistema" — shared Compose screens show a sample closet, allow adding a garment locally, and offer a sample outfit. Check: pure sample outfit rule tests and Android build.
S3. "La app requiere internet. SQLite vive únicamente en el servidor; el cliente no tendrá base local sincronizable en esta versión." — sample state is explicitly temporary prototype state, not a syncable or production persistence system. Check: readback of UI and README.
S4. "Para compilar, ejecutar y firmar la app iOS se necesitará macOS con Xcode." — include iOS target setup where feasible; iOS build is unavailable on this Windows machine. Check: configuration readback; no false iOS proof.

## Tasks
T1 [S1-S4] inline; status: pending delivery strategy before commit; commit: pending; risk: item 5 (new Android/Kotlin build dependencies and environment).

## Log
L1: ah vale listo bueno empecemos entonces
L2: Context: accepted preceding proposal to first build an APK prototype with sample data and no deployment. The November 24 MVP document is the source for the quoted prototype scope. Previous Expo work is absent from this checkout. Windows Android SDK and Gradle caches are present; no remote downloads are authorized. Planned checks run offline.
L3: Scaffold and sample UI/rule/tests written. `git diff --check` passed. Offline test invocation `gradle --offline --no-daemon :composeApp:allTests` failed before compilation because the Android KMP plugin marker `com.android.kotlin.multiplatform.library:9.4.1` is not cached. Neither a RED test nor an APK is verified. Do not commit or claim completion until dependency resolution, tests, APK build, and independent item-5 verification can run. Current authored change estimate exceeds ~400; choose chain strategy before the next work-unit commit under `ask-on-risk`.
L4: User authorized public Gradle dependency downloads, then requested autoskills.sh skills first. Installed the 15 skills shown in their image for this repository, with no credentials, verified all 15 bundle hashes, and refreshed `.atl/skill-registry.md` (61 indexed skills). Installer writes `.agents/skills/` and `skills-lock.json`; Kotlin CocoaPods/SPM skill is flagged by the registry for destructive example commands and Swift concurrency has tracking-link warnings, neither was executed.
L5: After enabling Android host tests and setting JVM bytecode target 17 without requiring a locally absent JDK 17 toolchain, `:composeApp:testAndroidHostTest` passed; `:androidApp:assembleDebug` produced a 9,846,975-byte APK. Independent item-5 verification found S1-S4 met within the prototype scope and proposed boundary tests; added them and reran offline test+build, with 4 tests, 0 failures, 0 errors. `git diff --check` passed. No Android device interaction or iOS build was performed. Verifier scratch directory was removed. Test-first RED was unavailable before implementation because offline Gradle plugin resolution failed. Current authored line count ~485; `ask-on-risk` requires a chain strategy choice before committing.
