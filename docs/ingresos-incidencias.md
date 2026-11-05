# Ingresos slice: errors and open points found while building it

Notes on what went wrong, or was left open, while building the ingresos slice (parts 1–3, branch
`feature/ingresos`). Everything below happened in this session; nothing is inferred.

## 1. Errors hit during the build (all fixed)

| # | Where | Error | Cause | Fix |
|---|---|---|---|---|
| 1 | Shell, writing `IngresoDetalleUiState.kt` / `IngresoDetalleViewModel.kt` with a heredoc | `unexpected EOF while looking for matching '` and neither file was written | Quoting problem in the long shell heredoc (it contained apostrophes in comments) | Checked with `ls` that the files did not exist and rewrote them with the Write tool. Lesson: use Write for long Kotlin files, not shell heredocs |
| 2 | `IngresoDetalleScreen.kt:200` (`:shared:compileKotlinJvm`) | `Unresolved reference 'campos' on receiver of type 'ErrorAuth'`, followed by type-inference errors in the same expression | `DialogoRegistrarCobro.errorServidor` is typed `ErrorAuth?`; `campos` exists only on `ErrorAuth.Validacion` | Narrowed with `as? ErrorAuth.Validacion` and fell back to `errorServidor.mensaje()` when there is no field map |
| 3 | `IngresoDetalleViewModelTest.kt:159` (`:shared:compileTestKotlinJvm`) | `TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR` on `assertEquals(listOf(9L to LocalDate(...)), ingresos.cobrosRegistrados)` | The fake stores `Pair<Long, LocalDate?>`, the expected list was inferred as `Pair<Long, LocalDate>` | Declared the expected type explicitly: `listOf<Pair<Long, LocalDate?>>(...)` |

None of these reached a commit: every commit builds and passes `:shared:jvmTest`.

## 2. Things checked instead of guessed

- **"Today" in kotlinx-datetime 0.8.0.** The project's version moved `Clock` to `kotlin.time`. I read
  the jar with `javap` and confirmed `todayIn(kotlin.time.Clock, TimeZone)`, so the code is
  `Clock.System.todayIn(TimeZone.currentSystemDefault())`.
- **Request body of `registrarCobro` without a date.** The contract requires the body, so the client
  must send `{}`. The default `Json` omits null defaults, and `IngresoDtosTest` /
  `IngresoRepositoryImplTest` assert `{}` explicitly.

## 3. Decisions I made that the prompts did not specify

- `EstadoCobro.DESCONOCIDO` is my name for the "safe fallback" of an unknown `estadoCobro`.
- A **blank** date in the "Registrar cobro" dialog is treated as invalid (it is not sent as "no
  date"), although the repository can omit the date.
- An ingreso whose estado is `DESCONOCIDO` gets neither "Registrar cobro" nor "Revertir cobro".
- The create form requires a cliente, as instructed, although the contract marks `clienteId` as
  optional on `CrearIngresoRequest`.
- The cobro date is not compared with `fechaEmision` on the client: the backend decides (409).
- `fechaHoraSinZona` in `GastoMapper.kt` changed from `private` to `internal` so the ingreso mapper
  reuses it (the only change to existing gastos code).

## 4. Known limitations and open points

- **Not verified against the real backend.** Everything here is covered by unit tests (fake
  repositories, Ktor MockEngine) and compiles for Android and Desktop. No window was opened, so the
  screens, the navigation after creating an ingreso (form replaced by the detail) and the "create a
  cliente" shortcut have not been seen running.
- **No edit of ingresos.** The backend has no `PUT`/`PATCH` on `/api/ingresos/{id}`.
- **No server-side pending filter.** Todos / Pendientes / Cobrados are applied on the client over
  the full list.
- **Cliente name is resolved on the client.** `IngresoResponse` only has `clienteId`, so names come
  from `ClienteRepository.listar()`. A cliente missing from that list (for example deactivated and
  not returned) shows "Cliente no disponible"; if the clientes call fails, rows have no name.
- **Environment mismatch at the start.** The initial git snapshot said the branch was
  `feature/gastos`, but the repository was on a clean `main` (gastos already merged). The slice was
  branched from an up-to-date `main`, as the prompt required.
- **Possible CRLF noise.** Git warns `LF will be replaced by CRLF` for the new files; the repository
  uses CRLF. Not an error, but expect those warnings on commit.
