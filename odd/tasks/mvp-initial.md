# Digital Closet initial MVP
Branch: `feat/mvp-initial`. Delivery: `ask-on-risk`; forecast: >1600 authored lines across reviewable work units. Runner: Gradle 9.6, Android SDK 36; iOS requires macOS/Xcode.
Objective: Evolve the merged sample-data prototype toward the authorized Kotlin Multiplatform private-beta MVP, locally first; no deployment, store publishing, or production credentials.

## Specs
S1. "bien ahora podemos empezar con la implementacion del mvp inicial?" — start implementing the MVP defined in `MVP_Closet_Digital_Kotlin_Multiplatform.md`, preserving its explicit scope and exclusions. Check: progress against S2-S7.
S2. "Crear Ktor Server con `GET /health` y una prueba de ruta." and "Definir los primeros DTOs de autenticación y perfil en `contracts`." — create the modular JVM API and multiplatform DTO module before feature implementation. Check: host route test, contracts compilation, Android regression.
S3. "Implementar registro, login, renovación de sesión y pruebas de autorización." and "Solo ve sus prendas, fotos, outfits y preferencias." — authenticated owner isolation and revocable sessions. Check: server integration and cross-owner denial tests.
S4. "Completar una primera funcionalidad vertical: cargar foto, registrar prenda, verla en el clóset, editarla y archivarla." and "Puede añadir una foto y registrar, corregir, archivar o eliminar una prenda." — private photo and garment flow; do not accept a client-provided owner as authority. Check: upload authorization, garment CRUD and object cleanup tests.
S5. "Implementar generación de outfits, sustitución de una pieza y feedback; cubrir las reglas con pruebas." and "`NEVER_SUGGEST_AGAIN` evita que reaparezca la misma composición." — server-side deterministic recommendation with exact-combination exclusions. Check: generation, replacement, ranking, explanation and feedback tests.
S6. "Puede revisar y administrar ideas de prendas complementarias." and "Puede solicitar la eliminación de su cuenta y de sus fotos." — wishlist and deletion process. Check: owner-scoped API tests and cleanup evidence.
S7. "**Primer hito revisable:** una persona puede crear una cuenta, agregar prendas con foto, verlas en su clóset y recibir al menos una combinación válida explicada." — wire Android app to actual API, retaining the demo only as a clearly marked prototype path until it is replaced. Check: client/server integration and Android APK; iOS proof pending macOS/Xcode.

## Tasks
T1 [S1-S2] inline; status: blocked on MVP dependency-download authorization; commit: pending; risk: item 5 (new server build dependencies).
T2 [S3] inline; status: pending; commit: pending; risk: item 2 (auth, sessions, access control).
T3 [S4] inline; status: pending; commit: pending; risk: item 2 (private media and ownership).
T4 [S5] inline; status: pending; commit: pending; risk: none expected (pure recommendation logic).
T5 [S6] inline; status: pending; commit: pending; risk: item 1/2 (account data deletion, privacy).
T6 [S7] inline; status: pending; commit: pending; risk: item 2 (mobile credentials and private photos).

## Log
L1: bien ahora podemos empezar con la implementacion del mvp inicial?
L2: The referenced Spanish MVP document was provided earlier in this conversation. At resume, prior prototype is already merged on `main` as `9dc476d` via PR #1 despite its stale historical task status. Current untracked `gradle/wrapper/`, `gradlew`, `gradlew.bat`, and `mitm_mcp_traffic.db` predate this task: preserve them and do not include them in commits. The global receipt-driven review switch is on. Start with the local server/contracts health slice; media provider and deployment choices are deferred until required. Gradle public dependency download approval from the prototype was limited to that work, so start offline for new dependencies.
L3: Added `contracts` serializable auth/profile/health/error DTOs, a local Ktor/JVM server exposing `GET /health` and `/api/v1/health`, route and contract tests, Gradle catalog wiring and README guidance. `git diff --check` passed; prior untracked files preserved. The first offline test invocation failed at Gradle plugin resolution before tests compiled because Kotlin serialization plugin 2.3.20 is not cached; no behavioral RED or GREEN can be claimed. Public Gradle repositories for the MVP require fresh remote-operation authorization before functional proof. No commit or native review started.
