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

Guidance for Claude Code when working in this repository. Read this before making changes.

## What this project is

The client app for GestionFacturas: a financial-control tool for freelancers and
self-employed people (expenses, income, clients, and a dashboard with profit and
per-client profitability). Guiding principle: **organization and visibility, never tax
advice or official invoicing**.

This repo is ONLY the client. All business logic (profit, totals, cobro transitions, IVA
figures, access control) lives in the Spring Boot backend (`GestionFacturas-api`). The client
displays data, sends the user's actions, and handles auth. Do NOT re-implement backend rules
here.

Targets: **Android + Desktop**, sharing one Compose Multiplatform UI. Desktop first; mobile
layout adaptation comes later. iOS is NOT a target (no Mac). The web version is a separate
Angular project, not part of this repo.

## Gradle modules

```
:shared      ALL shared code (data, domain, ui, di). Source sets: commonMain, androidMain,
             jvmMain (this is the Desktop/JVM target), commonTest, jvmTest, androidHostTest.
             Dependencies are declared in shared/build.gradle.kts.
:androidApp  Thin Android entry point (MainActivity, AndroidManifest, Android app config).
:desktopApp  Thin Desktop entry point (main function, packaging).
```

Both app modules only call into `:shared`. Do NOT put feature code in the app modules.
Platform-specific implementations (expect/actual) go in `shared/src/androidMain` and
`shared/src/jvmMain`. Android-only settings (e.g. the debug cleartext config) live in
`:androidApp`.

## Tech stack

- Kotlin Multiplatform + Compose Multiplatform (shared UI, Material 3)
- Ktor Client 3.6.x (content-negotiation + kotlinx.serialization JSON + Bearer auth plugin).
  HTTP engine: OkHttp on androidMain and jvmMain; common code builds the client with
  `HttpClient { ... }` and Ktor picks the engine on the classpath. (Ktor 3.6 also offers the
  `ktor-client-engine-defaults` artifact; not adopted yet.)
- kotlinx.serialization, kotlinx.coroutines, kotlinx-datetime 0.8.0 (`LocalDate` / `LocalDateTime`; no `java.time` in commonMain)
- Koin 4.2.2 (core, compose, compose-viewmodel) — modules in `di/`, started by `initKoin()`
- Multiplatform Settings 1.3.0 (stores the session: access token, refresh token, user id and
  email; plus the theme preference under `ui.tema`; no offline cache for now). Its last release is from Nov 2024; it compiles and builds
  with Kotlin 2.4.20, but has not been run yet (tests use an in-memory fake, and Android's no-arg
  auto-initialization is unverified at runtime); if it ever conflicts, switch to AndroidX
  DataStore (multiplatform).
- Navigation Compose 2.10.0-rc01 (org.jetbrains.androidx.navigation, official, multiplatform) —
  isolated in `ui/navigation/`. It is a RELEASE CANDIDATE: the latest stable (2.9.2) is built on
  lifecycle 2.9.x / Compose 1.8 and does not match this project's lifecycle 2.11.0 / CMP 1.12.1.
  Move to the stable 2.10.x as soon as it is released. Navigation 3 was considered: the CMP docs
  do not recommend it for new projects, so Navigation Compose stays.
- Multiplatform ViewModel/Lifecycle (androidx, lifecycle 2.11.0)
- Kermit 2.2.0 (logging; never given tokens, passwords or bodies)
- Test only (commonTest): kotlinx-coroutines-test 1.11.0, ktor-client-mock 3.6.0
- Gradle Kotlin DSL with the version catalog (`gradle/libs.versions.toml`)

Do not add Room, SQLDelight or any local database unless asked: the app is online-only for
now.

## Architecture — MVVM + Clean (light)

Decided and closed. Do not switch pattern or add layers without being asked.

```
ui  ->  domain  <-  data
```

- **ui**: Compose screens + ViewModels. Depends only on `domain`. NEVER imports from `data`.
- **domain**: plain Kotlin (no Ktor, no Compose, no Settings, no `java.*`): models and
  repository INTERFACES. Use cases only when one operation orchestrates more than one data
  source or is reused by several ViewModels. There are none yet. Logout is NOT a use case:
  `AuthRepository` already owns both the remote API and the local token storage, so it revokes
  the refresh token and clears the local session itself. Do NOT create pass-through use cases
  that only call a repository.
- **data**: repository implementations, Ktor services, DTOs, token storage, DTO -> model
  mappers. Implements the `domain` interfaces.

### Models: keep them separate
- DTO (`data/remote`, `@Serializable`) = the API contract.
- Domain model (`domain/model`) = plain data classes the rest of the app uses.
- `UiState` (one per screen) = what the screen renders.
  Mappers live in `data`. Screens never see DTOs.

### MVVM rules
- A ViewModel exposes ONE immutable `StateFlow<UiState>`; the screen only renders it and
  forwards user actions to the ViewModel.
- One-shot effects (navigate after login, show a message) are modeled INSIDE the `UiState`
  (e.g. a field the screen consumes and the ViewModel clears), not in fire-and-forget
  channels that can be lost or replayed.
- Composables stay as stateless as possible (state hoisting). No logic or network calls in
  composables.
- Network calls are `suspend` functions launched from the ViewModel scope. Never block the UI.
- ViewModels get dependencies from Koin. Every ViewModel MUST be registered in `moduloUi`
  (`di/Modulos.kt`): the unit tests build ViewModels by hand with fakes, so a missing registration
  only fails at runtime, when `koinViewModel()` composes (this happened with the dashboard).

## Package structure

Root package: `com.fabio.gestionfacturas`, under
`shared/src/commonMain/kotlin/com/fabio/gestionfacturas/`.

Layers at the top, feature areas only inside `ui/`:

```
shared/src/commonMain/kotlin/com/fabio/gestionfacturas/
├── data/
│   ├── remote/      ApiClient (Ktor), @Serializable DTOs, one service per resource
│   ├── local/       token storage (Multiplatform Settings); expect fun crearFileSaver() (actuals in jvmMain / androidMain)
│   └── repository/  implementations of the domain interfaces + mappers
├── domain/
│   ├── model/       data classes (Gasto, Ingreso, Cliente, ResumenPeriodo...)
│   ├── repository/  repository interfaces
│   └── usecase/     only for multi-source orchestration (e.g. LogoutUseCase)
├── ui/
│   ├── navigation/  routes + outer graph (Login, Registro, Shell) + ShellScreen (sidebar + content
│   │                area with its own nested NavHost) + ShellViewModel (email, logout) + TemaViewModel (light/dark)
│   ├── login/ clientes/ gastos/ ingresos/ dashboard/   (Screen + ViewModel + UiState each;
│   │                clientes/ has the list and the create/edit form; gastos/ has the list, the
│   │                read-only detail (with the review action) and the create/edit form;
│   │                ingresos/ has the list (client-side filter chips), the detail with the cobro
│   │                actions and a create-only form; dashboard/ is one screen with the period
│   │                selector, amount mode toggle and three independent sections)
│   └── common/      shared composables: Estados (loading / empty / error + retry), BarraTitulo
│                    (title + explicit Volver), Formulario (form container, fields, buttons),
│                    MensajesError (the single place that maps errors to texts), BarraDesplazamiento
│                    (Desktop scrollbar; expect/actual, no-op on Android)
└── di/              Koin modules
shared/src/androidMain , shared/src/jvmMain   platform-specific code only (expect/actual):
                     base URL, Settings factory, file picker later
```

## Backend contract (what this client talks to)

Base URL differs per platform: Desktop `http://localhost:8080`; Android emulator
`http://10.0.2.2:8080`; physical phone = the PC's LAN IP. Keep it configurable per platform.
It lives in `expect fun urlBaseApi()` (`data/remote/UrlBase.kt`), with the actuals in
`shared/src/jvmMain/.../UrlBase.jvm.kt` and `shared/src/androidMain/.../UrlBase.android.kt`
(edit the Android one to test on a physical phone).

`docs/api-contract.md` is the source of truth for DTO shapes (audited from the backend's
`/v3/api-docs`). Update it whenever the backend changes.

- Auth (public by convention; the OpenAPI does not mark them): `POST /api/auth/register`
  (returns the user, no tokens), `/login`, `/refresh` (returns only a new `accessToken`),
  `/logout`. Also `GET /api/health`.
- Protected (Bearer JWT): `/api/gastos` (GET list, POST; `GET /{id}`; `POST /digitalizar`
  multipart, part `archivo`), `/api/ingresos` (GET list, POST; `GET /{id}`;
  `POST /{id}/cobro` registers, `DELETE /{id}/cobro` reverts), `/api/clientes` (GET list,
  POST; `GET|PUT|DELETE /{id}`, DELETE = deactivate), `/api/categorias` (GET list),
  `/api/dashboard/resumen?desde&hasta`, `/api/dashboard/rentabilidad-clientes?desde&hasta`,
  `/api/dashboard/pendientes-cobro`, `/api/exportacion/excel?desde&hasta` (binary .xlsx, see the contract).
- Gastos also have `PUT /{id}` (edit; the only way to set a gasto's category and `deducible`) and
  `POST /{id}/revisar`.
- NOT in the API (as of the 2026-10-27 audit): delete of gastos, edit of ingresos, a pending
  filter on the ingresos list, category on gasto creation. See the gaps table in the contract.
- Tokens: access token ~15 min, refresh token ~7 days (revocable server-side).
- Status codes: 400 validation/domain error, 401 bad credentials, 404 not found (also for
  another user's resource), 409 conflict (duplicate email/NIF, invalid cobro transition),
  429 rate limited (with `Retry-After`).
- Error bodies are JSON; domain errors use an `error` message field and validation errors
  are a map of field -> message.
- Rate limits: login 10/min per IP, register 5/hour per IP, everything else 50/min per user.
  Handle 429 with a friendly message; do not retry in a tight loop.
- Do NOT guess DTO field names. Copy the exact request/response shapes from
  `docs/api-contract.md` (or the backend's `/v3/api-docs` / Swagger UI if the file looks stale).
  The login response changed when refresh tokens were added.

## Authentication on the client

Implemented flow (all in `data/`):
- `ApiClient` builds TWO Ktor clients: `plain` (no auth plugin; used by `AuthApi` for
  `/api/auth/*` and for the refresh call) and `authenticated` (Bearer plugin; for every protected
  endpoint of the later slices). Both: JSON with `ignoreUnknownKeys`, timeouts (10 s connect,
  30 s request/socket), base URL from `urlBaseApi()`. There is deliberately NO Ktor logging plugin.
- Tokens (access, refresh, user id, email) live in `TokenStorage` (Multiplatform Settings).
  `EstadoSesion` is the single "is there a session" flag, exposed as `AuthRepository.sesionActiva`;
  it starts true when a refresh token is stored.
- Bearer: `loadTokens` reads `TokenStorage`; on a 401, `refreshTokens` calls
  `POST /api/auth/refresh` through the PLAIN client with the stored refresh token. That endpoint
  returns only an `accessToken`, so the stored refresh token is KEPT. If refreshing fails for any
  reason (including no network), the stored tokens are cleared and `sesionActiva` becomes false.
  `sendWithoutRequest { true }` is Ktor 3.6's default and is kept explicit: Ktor only refreshes
  for requests that already carried the token (`ApiClientRefreshTest` fails if that changes).
- `AuthRepositoryImpl.logout()` revokes the refresh token on the backend (best effort) and
  ALWAYS clears the local tokens and Ktor's cached Bearer tokens (`HttpClient.clearAuthTokens()`,
  Ktor 3.4+), even if the network call fails. Login also clears that cache so a previous
  user's token is never reused.
- Registration returns no tokens: `registrar` registers and then logs in with the same
  credentials.
- Errors are the sealed `ErrorAuth` (domain): 401 credentials, 429 rate limited (with the
  numeric `Retry-After` seconds if present), 400 validation (field -> message map; a plain
  `{"error": ...}` body is kept under the key `error`), 409 email already registered, I/O or
  timeout = server unreachable, anything else unexpected.
- Navigation starts on the Shell (sidebar + content area) if a session exists, else Login; when
  `sesionActiva` turns false it returns to Login clearing the back stack, from any screen.
- KNOWN: Multiplatform Settings does NOT encrypt. Acceptable for this portfolio project;
  secure storage (Android Keystore, etc.) is a pending item.
- 401 vs 403: protected endpoints return 401 for a missing or invalid token (VERIFIED against the
  running backend on 2026-10-27), with `WWW-Authenticate: Bearer` and the JSON body
  `{"error":"No autenticado. Inicia sesión de nuevo."}`. That 401 is what triggers Ktor's
  refresh; there is no need to handle 403. The client does not depend on the body or the header.
  The 401 for an EXPIRED token and the refresh-and-retry are not yet verified against the running
  backend (they rest on `SecurityConfig`'s `HttpStatusEntryPoint(UNAUTHORIZED)`).
- Never log passwords or tokens (Kermit prints whatever it is given).

## Money — DECIDED
Amounts are TEXT in the client, with NO arithmetic (the backend computes totals and profit).
`domain/model/Importe` is a value class over String: `Importe.de(texto)` validates user input
(digits, at most 2 decimals, comma or dot) and normalizes to a dot; `mostrar()` gives the comma
display, using string operations only. `data/remote/dto/ImporteSerializer` reads the JSON number
as its literal text (`121.00` keeps its zeros) and writes it back as a JSON NUMBER, not a string.
Amounts READ from the API are never validated (the internal constructor wraps the literal), so a
negative beneficio such as `-181.50` keeps its text, `mostrar()` gives `-181,50` and `esNegativo`
(string check on the leading minus, no arithmetic) drives the error color. `Importe.de` stays
strictly non-negative: it is only for form input. Never use `Double`/`Float` for money.

## Code conventions
- Domain terms in Spanish (Gasto, Ingreso, Cliente, ResumenPeriodo...); technical suffixes
  in English (Repository, ViewModel, UiState, Screen). Same convention as the backend.
- No `java.*` imports in `commonMain`.
- Prefer immutable `data class`es and `val`. Make nullability explicit; a field the JSON may
  omit must be nullable or have a default.
- Distinguish error cases in the UI (wrong credentials, rate limited, server unreachable);
  do not collapse them into one generic error.
- Mask the password field (`PasswordVisualTransformation`).

## Platform gotchas
- Android blocks cleartext HTTP by default and the dev backend is `http://`. Allow cleartext
  ONLY for debug builds (network security config / `usesCleartextTraffic` limited to debug),
  configured in `:androidApp`. Done: `androidApp/src/debug/` holds the manifest attribute and
  `res/xml/network_security_config.xml` (cleartext only for `10.0.2.2` and `localhost`); the main
  manifest only declares `INTERNET`. The merged release manifest was checked: no
  `networkSecurityConfig` and no `usesCleartextTraffic`. A physical phone on a LAN IP would need
  that IP added to the debug config.
- Desktop needs `kotlinx-coroutines-swing` for `Dispatchers.Main`; if a ViewModel fails with
  "Module with the Main dispatcher is missing", that is the cause.
- Desktop has no system back button: provide explicit back navigation in the UI.
- CORS does not apply to this client (browsers only).

## Testing
- Tests live in `shared/src/commonTest` (run on every target); run them with
  `./gradlew :shared:jvmTest` (see `./gradlew :shared:tasks` for the full list).
- Prefer TDD for new logic. The owner writes the TDD tests for genuinely new rules; Claude
  Code implements against them and does the mechanical parts.
- ViewModels are tested with FAKE repositories (implement the domain interface), no network.
- Tests are the contract: do NOT modify a test to make it pass; adapt the code.
- Passing unit tests do not prove end-to-end behavior. Verify against the real running
  backend and compare with its real responses.

## Current status
Project generated from the Android Studio KMP template with the three modules above. The
template package is renamed to `com.fabio.gestionfacturas`; the API contract is documented in
`docs/api-contract.md`.
The **login / registro vertical slice is DONE** (code, unit tests, builds): networking base,
token storage, authentication with automatic refresh, session handling, Login and Registro
screens (Spanish, Material 3, Desktop + Android).
Unit tests: `./gradlew :shared:jvmTest` (DTOs, repository and refresh flow with Ktor MockEngine,
ViewModels with a fake repository). Desktop run against the real backend: login, registro,
logout, session restore on restart and wrong credentials confirmed working by the owner
(`./gradlew :desktopApp:run`; mind the login rate limit of 10/min). NOT yet verified against the
real backend: backend down (server unreachable message) and the token refresh.
The **clientes vertical slice is DONE** (code, unit tests, builds; not yet run against the real
backend): domain, data, the logged-in shell (sidebar with Dashboard, Clientes, Gastos and Ingresos,
email and logout), the list (loading / empty / error / content, refresh,
deactivate with confirmation), and one create/edit form (NIF editable only on create; required
fields checked on the client, everything else shown per field from the backend's 400 map; 404 on
edit returns to the list with a message). The list also shows deactivated clientes (tagged
"Inactivo"). Unit tests cover the list and form ViewModels with a fake repository.
The **gastos vertical slice is DONE** (code, unit tests, builds; not yet run against the real
backend): `Importe` money type, domain, data (`GastoApi`, `CategoriaApi`), list (BORRADOR rows stand
out as pending review), read-only detail, one create/edit form (cliente selector from the active
clientes; category selector and `deducible` switch on edit only, because the create request has
neither; fecha as a `yyyy-MM-dd` text field, no date picker; required `total` checked on the client,
everything else per field from the backend's 400 map) and the "Marcar como revisado" action with
confirmation (enabled for BORRADOR / PROCESADA only). Invoice digitization (`/digitalizar`) is NOT
built. `GastoResponse` has no `total`, so the list shows base imponible and IVA, and the edit form
cannot prefill the total: the user types it again. Unit tests cover the list, detail and form
ViewModels with fake repositories, and the repositories with MockEngine.
The **ingresos vertical slice is DONE** (code, unit tests, builds; not yet run against the real
backend): domain (`Ingreso`, `EstadoCobro` with a `DESCONOCIDO` fallback for unknown values),
data (`IngresoApi`, DTOs with `Importe`), list with Todos / Pendientes / Cobrados chips applied
CLIENT-SIDE on `estadoCobro` (the list endpoint has no filter), read-only detail with "Registrar
cobro" (dialog with a `yyyy-MM-dd` date defaulting to today; PENDIENTE only) and "Revertir cobro"
(with confirmation; COBRADA only), and a create-only form (required cliente selector with ACTIVE
clientes, with a shortcut to create one when there are none; `total` required; `moneda` defaults to
EUR; no automatic calculation of base / IVA / total). There is NO edit of ingresos: the backend has
no endpoint for it (known limitation of this version). `IngresoResponse` only carries `clienteId`,
so the cliente NAME is resolved in the client from `ClienteRepository.listar()` (list and detail,
best effort); a cliente missing from that list (e.g. deactivated) shows a neutral placeholder. A 409
on a cobro operation shows the backend's message next to the actions; a 404 returns to the list
with a message. Unit tests cover the ViewModels with fake repositories and the repository / DTOs
with MockEngine.
The **dashboard vertical slice is DONE** (code, unit tests, builds; not yet run against the real
backend): it is the first sidebar entry and the shell's start destination (so the screen after
login). Domain `ResumenPeriodo` (ten `Importe` figures), `RentabilidadCliente`, `PendientesCobro`
and `DashboardRepository`; data `DashboardApi` on the authenticated client. The period selector has
the presets "Este mes", "Trimestre actual" (default) and "Este año" (the whole calendar month /
quarter / year, computed with kotlinx-datetime from an injectable "today") plus "Personalizado" (two
`yyyy-MM-dd` fields and "Aplicar"; the range is validated on the client, `desde <= hasta`, and
nothing is requested while typing). The amount mode toggle "Con IVA" / "Sin IVA" only SELECTS the
`*Total` or `*Base` fields of the backend response (no arithmetic, no request). Three sections load
independently, each with its own loading / error-with-retry / content states: resumen (five cards;
a negative beneficio is shown in the error color), rentabilidad por cliente (backend order, never
sorted or compared on the client; the entry with null `clienteId` is shown as "Gastos generales (sin
cliente)") and pendientes de cobro (tapping an item opens the ingreso detail; the dashboard reloads
when the detail reports a change). The resumen and rentabilidad follow the period; pendientes does
not. `RentabilidadClienteResponse` has NO client name, so names are resolved in the client from
`ClienteRepository.listar()` (best effort, neutral placeholder if missing), like the ingresos.
Unit tests cover the repository / DTOs with MockEngine and the ViewModel with a fake repository.
The **dark mode is DONE** (code, unit tests, builds; not yet run on screen): `PreferenciaTema`
(`SISTEMA` / `CLARO` / `OSCURO`) in the domain, `TemaRepository` implemented over Settings
(`TemaRepositoryImpl`, key `ui.tema`; a missing or unknown value is `SISTEMA`), `TemaViewModel`
and an `IconButton` in the shell sidebar that toggles ONLY between light and dark (sun icon when
light, moon when dark; `SISTEMA` exists only until the first choice and follows the OS via
`isSystemInDarkTheme()`). `App` applies `darkColorScheme()` / `lightColorScheme()`; no colors are
hardcoded in the screens. The sun and moon are `ImageVector`s drawn in `ui/navigation/IconosTema.kt`
because `material-icons-extended` is not a dependency.
The **dashboard export is DONE** (code, unit tests, builds; not yet run against the real backend): an
"Exportar a Excel" button next to the period controls calls `GET /api/exportacion/excel` with the APPLIED
period (`ExportApi` on the authenticated client, bytes read as `ByteArray`, 120 s timeout for that call only,
file name from `Content-Disposition` with a fallback built from the range). The domain has
`ExportRepository` and the generic `FileSaver` (`Guardado` / `Cancelado` / `Error`). Desktop saves with a native
AWT `FileDialog` proposing the name in Downloads or Documents (write on `Dispatchers.IO`); the Android actual is a
STUB that returns an Error (see Pending). The button is disabled while exporting or while the custom range is
invalid; a caption says the file always has Total and Base figures. Cancel shows no message.
All planned slices are done; what remains is in "Pending / deferred".

## Pending / deferred
- KNOWN ISSUE (clientes): creating a cliente whose NIF already exists returns 409, and the backend
  counts deactivated clientes too. There is no reactivate endpoint, so a deactivated cliente's NIF
  cannot be reused or recovered from the client. The form says so on a 409; the real fix needs a
  backend change (reactivate endpoint or excluding deactivated clientes from the NIF check).
- Navigation Compose is on a release candidate (2.10.0-rc01): update to the stable 2.10.x when
  it exists. Navigation 3 was checked and not adopted (see Tech stack); navigation is isolated
  in `ui/navigation/` so a later switch stays contained.
- Secure token storage.
- Mobile layout adaptation (bottom navigation) after the desktop version works.
- Android `FileSaver`: the actual in `androidMain` only returns an Error. Finish it with the Storage Access
  Framework (`CreateDocument`, needs an Activity result launcher) in the Android phase.
- Invoice digitization upload (`POST /api/gastos/digitalizar`): file picker on desktop, camera/gallery
  on Android (expect/actual).
- Gastos: the backend does not return `total` in `GastoResponse` (open item in the API contract), and
  does not document whether REVISADA / EXPORTADA gastos can be edited; "Editar" is offered for every
  estado and the backend's 409 is explained in the form. A date picker was not adopted because it was
  not verified on Desktop.
- Optional offline cache (Room KMP) only if a real need appears.

## Working style
- Focused, minimal changes for the task. Do not refactor unrelated code.
- Run the app on Desktop with `./gradlew :desktopApp:run` and the tests before finishing.
- Show a summary of changed files before considering a task done.
- If a request would violate the architecture above, flag it instead of doing it.
- When verifying behavior, report evidence (actual responses, exact discrepancies); do not
  silently patch code during a verification task.

## Git workflow
- Before a new feature/change, create a branch: `git checkout -b feature/<desc>`
  (prefixes: feature/, fix/, refactor/, test/, docs/). Never commit feature work to main.
- Focused commits. Do not merge to main or delete branches unless explicitly asked.

