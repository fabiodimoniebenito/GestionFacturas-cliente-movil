# GestionFacturas – cliente móvil

Kotlin Multiplatform + Compose Multiplatform client.

## Modules
- `:shared` – common UI/logic (commonMain, androidMain, jvmMain, commonTest, jvmTest, androidHostTest)
- `:androidApp` – Android entry point (`MainActivity`)
- `:desktopApp` – Desktop (JVM) entry point (`main`)

## Rules
- Root package: `com.fabio.gestionfacturas`.
- Do not add or upgrade dependencies without being asked.
- Do not change `rootProject.name` in `settings.gradle.kts`.
- Use `git mv` when moving files to preserve history.
- Do not push, merge or delete branches unless asked.

## Commands
- `./gradlew :shared:jvmTest`
- `./gradlew :androidApp:assembleDebug`
- `./gradlew :desktopApp:run` (launches a window; run manually)
