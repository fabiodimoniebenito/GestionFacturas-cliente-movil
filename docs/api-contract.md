# API contract

Source: `GET http://localhost:8080/v3/api-docs` (OpenAPI 3.1.0, `info.version` = `v0`), fetched
2026-10-19. Field names and types below are copied from that document. This file is the source
of truth for DTO shapes in this client and **must be updated whenever the backend changes**.
The backend lives in another repository (`GestionFacturas-api`); this client only documents it.

## Conventions seen in the document

- Base URL: `http://localhost:8080` (Desktop). Other platforms: see `CLAUDE.md`.
- A global `security: [bearerAuth]` (HTTP bearer, JWT) is declared for the whole API, and **no
  operation overrides it**. The document therefore does NOT mark any endpoint as public. The
  "Auth" column below says `public (per backend convention, NOT verified from the OpenAPI)` for
  `/api/auth/*` and `/api/health`, and `Bearer` for the rest.
- Every operation documents only a `200 OK` response. Creates return `200`, not `201`.
- Response content type is declared as `*/*`; request bodies are `application/json` (except
  digitalizar: `multipart/form-data`).
- No list endpoint declares pagination, sorting or filter parameters.
- `int64` ids are JSON integers. Dates are `string` / `format: date` (`yyyy-MM-dd`); timestamps are
  `string` / `format: date-time`.
- Enumerations are not declared: `estadoCobro` and `estado` are plain `string`.

## 1. Endpoints

Auth legend: **P\*** = public per backend convention (not verified from the OpenAPI); **B** = Bearer.

| Method | Path | Auth | Request | Query | Response (200) |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | P\* | `RegistrarRequest` | – | `UsuarioResponse` |
| POST | `/api/auth/login` | P\* | `LoginRequest` | – | `LoginResponse` |
| POST | `/api/auth/refresh` | P\* | `RefreshRequest` | – | `RefreshResponse` |
| POST | `/api/auth/logout` | P\* | `LogoutRequest` | – | no body |
| GET | `/api/health` | P\* | – | – | `HealthResponse` |
| GET | `/api/clientes` | B | – | – | `ClienteResponse[]` |
| POST | `/api/clientes` | B | `CrearClienteRequest` | – | `ClienteResponse` |
| GET | `/api/clientes/{id}` | B | – | path `id` int64 | `ClienteResponse` |
| PUT | `/api/clientes/{id}` | B | `EditarClienteRequest` | path `id` int64 | `ClienteResponse` |
| DELETE | `/api/clientes/{id}` (operationId `desactivar`) | B | – | path `id` int64 | no body |
| GET | `/api/gastos` | B | – | – | `GastoResponse[]` |
| POST | `/api/gastos` | B | `CrearGastoRequest` | – | `GastoResponse` |
| GET | `/api/gastos/{id}` | B | – | path `id` int64 | `GastoResponse` |
| POST | `/api/gastos/digitalizar` | B | multipart: `archivo` (binary, required) | – | `GastoResponse` |
| GET | `/api/ingresos` | B | – | – | `IngresoResponse[]` |
| POST | `/api/ingresos` | B | `CrearIngresoRequest` | – | `IngresoResponse` |
| GET | `/api/ingresos/{id}` | B | – | path `id` int64 | `IngresoResponse` |
| POST | `/api/ingresos/{id}/cobro` | B | `RegistrarCobroRequest` (body required) | path `id` int64 | `IngresoResponse` |
| DELETE | `/api/ingresos/{id}/cobro` (operationId `revertirCobro`) | B | – | path `id` int64 | `IngresoResponse` |
| GET | `/api/categorias` | B | – | – | `CategoriaResponse[]` |
| GET | `/api/dashboard/resumen` | B | – | `desde` date (required), `hasta` date (required) | `ResumenPeriodoResponse` |
| GET | `/api/dashboard/rentabilidad-clientes` | B | – | `desde` date (required), `hasta` date (required) | `RentabilidadClienteResponse[]` |
| GET | `/api/dashboard/pendientes-cobro` | B | – | – | `PendientesCobroResponse` |

There are no other paths. In particular there is no `PUT`/`DELETE` on `/api/gastos/{id}`, no
`PUT`/`DELETE` on `/api/ingresos/{id}`, and no review endpoint for gastos.

### Request schemas

`RegistrarRequest` — required: `email`, `nombre`, `password`

| Field | Type | Constraints |
|---|---|---|
| email | string, format `email` | minLength 1 |
| nombre | string | minLength 1 |
| password | string | minLength 1 |

`LoginRequest` — required: `email`, `password`

| Field | Type | Constraints |
|---|---|---|
| email | string | minLength 1 |
| password | string | minLength 1 |

`RefreshRequest` / `LogoutRequest` — required: `refreshToken`

| Field | Type | Constraints |
|---|---|---|
| refreshToken | string | minLength 1 |

`CrearClienteRequest` — required: `nif`, `nombre`

| Field | Type | Constraints |
|---|---|---|
| nombre | string | minLength 1 |
| nif | string | minLength 1 |
| email | string, format `email` | optional |
| telefono | string | optional |

`EditarClienteRequest` — required: `nombre` (no `nif`: it cannot be edited)

| Field | Type | Constraints |
|---|---|---|
| nombre | string | minLength 1 |
| email | string, format `email` | optional |
| telefono | string | optional |

`CrearGastoRequest` — required: `total`

| Field | Type | Constraints |
|---|---|---|
| emisor | string | optional |
| fechaEmision | string, format `date` | optional |
| baseImponible | **number** (no format) | minimum 0, optional |
| iva | **number** (no format) | minimum 0, optional |
| total | **number** (no format) | minimum 0, **required** |
| moneda | string | optional |
| clienteId | integer int64 | optional |

`CrearIngresoRequest` — required: `total`

| Field | Type | Constraints |
|---|---|---|
| clienteId | integer int64 | optional |
| concepto | string | optional |
| fechaEmision | string, format `date` | optional |
| baseImponible | **number** (no format) | minimum 0, optional |
| iva | **number** (no format) | minimum 0, optional |
| total | **number** (no format) | minimum 0, **required** |
| moneda | string | optional |

`RegistrarCobroRequest` — no required fields

| Field | Type | Constraints |
|---|---|---|
| fechaCobro | string, format `date` | optional |

Multipart `digitalizar` body: object with `archivo` (`string`, format `binary`), required.

### Response schemas

The OpenAPI document declares **no `required` list on any response schema**, so every response
field below is, by the schema alone, of unspecified presence (see section 3).

`LoginResponse`: `accessToken` string, `refreshToken` string, `usuarioId` integer int64, `email` string.

`RefreshResponse`: `accessToken` string. (No new refresh token is returned.)

`UsuarioResponse`: `id` integer int64, `email` string, `nombre` string. (Register returns no tokens.)

`HealthResponse`: `status` string.

`ClienteResponse`: `id` int64, `usuarioId` int64, `nombre` string, `nif` string, `email` string,
`telefono` string, `activo` boolean.

`GastoResponse`: `id` int64, `usuarioId` int64, `categoriaId` int64, `clienteId` int64,
`emisor` string, `fechaEmision` date, `baseImponible` number, `iva` number, `moneda` string,
`deducible` boolean, `estado` string, `creadoEn` date-time.
**There is no `total` field in `GastoResponse`**, although `CrearGastoRequest` has one.

`IngresoResponse`: `id` int64, `usuarioId` int64, `clienteId` int64, `concepto` string,
`fechaEmision` date, `baseImponible` number, `iva` number, `total` number, `moneda` string,
`estadoCobro` string, `fechaCobro` date, `creadoEn` date-time.

`CategoriaResponse`: `id` int64, `nombre` string, `deduciblePorDefecto` boolean.

`ResumenPeriodoResponse` (all `number`): `facturadoTotal`, `facturadoBase`, `cobradoTotal`,
`cobradoBase`, `gastosTotal`, `gastosBase`, `beneficioCajaTotal`, `beneficioCajaBase`,
`beneficioFacturadoTotal`, `beneficioFacturadoBase`.

`RentabilidadClienteResponse`: `clienteId` int64, `resumen` `ResumenPeriodoResponse`.

`PendientesCobroResponse`: `ingresos` `IngresoPendienteResponse[]`, `total` number.

`IngresoPendienteResponse`: `id` int64, `clienteId` int64, `concepto` string, `fechaEmision` date,
`baseImponible` number, `iva` number, `total` number.

## 2. Money fields (JSON type exactly as declared)

Every money field is declared as `"type": "number"` **with no `format`** (not `float`/`double`,
not a string). The client-side representation is **not decided here** (pending owner decision).

| Schema | Money fields |
|---|---|
| `CrearGastoRequest` | `baseImponible`, `iva`, `total` (minimum 0) |
| `CrearIngresoRequest` | `baseImponible`, `iva`, `total` (minimum 0) |
| `GastoResponse` | `baseImponible`, `iva` (no `total`) |
| `IngresoResponse` | `baseImponible`, `iva`, `total` |
| `IngresoPendienteResponse` | `baseImponible`, `iva`, `total` |
| `PendientesCobroResponse` | `total` |
| `ResumenPeriodoResponse` | `facturadoTotal`, `facturadoBase`, `cobradoTotal`, `cobradoBase`, `gastosTotal`, `gastosBase`, `beneficioCajaTotal`, `beneficioCajaBase`, `beneficioFacturadoTotal`, `beneficioFacturadoBase` |

Not declared anywhere: scale/precision, currency semantics (`moneda` is a free `string`), and
whether the server serializes decimals as JSON numbers with trailing zeros.

## 3. Nullable / optional fields

- The document is OpenAPI 3.1 and **no schema uses `nullable`, a `["x","null"]` type array, or
  `oneOf` with `null`**. So nothing is explicitly marked nullable.
- **Requests** — required fields are listed in section 1 (`required` arrays). Everything else in a
  request is optional (omittable).
  - `RegistrarCobroRequest`: all fields optional, but the body itself is `required: true`
    (send at least `{}`).
- **Responses** — no `required` array exists on any response schema, so presence and nullability
  of every response field are **unspecified**. Likely-null fields by business meaning (NOT
  confirmed by the schema, treat as nullable/defaulted in the client until checked against real
  responses): `GastoResponse.categoriaId`, `GastoResponse.clienteId`, `GastoResponse.emisor`,
  `IngresoResponse.clienteId`, `IngresoResponse.fechaCobro`, `ClienteResponse.email`,
  `ClienteResponse.telefono`.

## 4. Error handling

**Per backend convention, NOT verified from the OpenAPI.** The document declares no error
responses (only `200`), no error schema, and no `Retry-After` header.

| Status | Meaning (per `CLAUDE.md`) |
|---|---|
| 400 | Validation or domain error |
| 401 | Bad credentials (login), or a missing / invalid / expired access token on a protected route |
| 404 | Not found (also used for another user's resource) |
| 409 | Conflict: duplicate email/NIF, invalid cobro transition |
| 429 | Rate limited, with `Retry-After` |

- Error bodies are JSON. Domain errors use an `error` message field; validation errors are a map
  of field -> message.
- Rate limits: login 10/min per IP, register 5/hour per IP, everything else 50/min per user.
- Correction: protected endpoints answer **401** (not 403) for a missing, invalid or expired token.
  The earlier "may answer 403" note was wrong. Source: the backend's `SecurityConfig` registers
  `HttpStatusEntryPoint(UNAUTHORIZED)`; this client's refresh flow relies on the 401. On backend
  `main` that 401 has an empty body and no `WWW-Authenticate` header; an open backend PR adds a JSON
  `{"error": ...}` body and `WWW-Authenticate: Bearer`. Read from the backend source and its own
  tests, not yet re-checked by this client against the running backend.

## 5. Gaps vs planned screens

Status is judged only from the paths and schemas above.

| Capability | Status | Notes |
|---|---|---|
| Register | AVAILABLE | `POST /api/auth/register` -> `UsuarioResponse` (no tokens; log in afterwards) |
| Login | AVAILABLE | `POST /api/auth/login` -> access + refresh token, `usuarioId`, `email` |
| Refresh | AVAILABLE | `POST /api/auth/refresh`; returns only `accessToken` (refresh token is not rotated) |
| Logout | AVAILABLE | `POST /api/auth/logout` with `refreshToken`; no response body |
| Clientes list | AVAILABLE | `GET /api/clientes`; whether inactive clientes are included is UNCLEAR (no filter param, `activo` is in the response) |
| Clientes create | AVAILABLE | `POST /api/clientes` (`nombre`, `nif` required) |
| Clientes edit | AVAILABLE | `PUT /api/clientes/{id}` (`nif` not editable) |
| Clientes deactivate | AVAILABLE | `DELETE /api/clientes/{id}` (operationId `desactivar`); no reactivate endpoint |
| Clientes detail | AVAILABLE | `GET /api/clientes/{id}` |
| Gastos list | AVAILABLE | `GET /api/gastos`; no filters/pagination |
| Gastos create | AVAILABLE | `POST /api/gastos`; request has no `categoriaId` |
| Gastos detail | AVAILABLE | `GET /api/gastos/{id}` |
| Gastos edit | MISSING | No `PUT`/`PATCH` on `/api/gastos/{id}` |
| Gastos review | MISSING | No review endpoint; `GastoResponse.estado` (string) hints at a state, but no operation changes it |
| Gastos delete | MISSING | No `DELETE` on `/api/gastos/{id}` |
| Ingresos list | AVAILABLE | `GET /api/ingresos` |
| Ingresos list with pending filter | MISSING | List takes no parameters. Workarounds exist but are not a filter: `GET /api/dashboard/pendientes-cobro` (separate, slimmer shape) or filter client-side on `estadoCobro` (values undeclared) |
| Ingresos create | AVAILABLE | `POST /api/ingresos` (`total` required) |
| Ingresos detail | AVAILABLE | `GET /api/ingresos/{id}` |
| Ingresos edit | MISSING | No `PUT`/`PATCH` on `/api/ingresos/{id}` |
| Ingresos registrar cobro | AVAILABLE | `POST /api/ingresos/{id}/cobro` with optional `fechaCobro` |
| Ingresos revertir cobro | AVAILABLE | `DELETE /api/ingresos/{id}/cobro` |
| Categorias list | AVAILABLE | `GET /api/categorias` |
| Category assigned to a gasto | UNCLEAR | `CrearGastoRequest` has no `categoriaId`; `GastoResponse` has `categoriaId` and `deducible`. No endpoint to set it, so it is presumably assigned server-side (e.g. by the digitalizar flow). Not stated in the schema |
| Dashboard resumen | AVAILABLE | `GET /api/dashboard/resumen?desde&hasta` (both required) |
| Dashboard rentabilidad-clientes | AVAILABLE | `GET /api/dashboard/rentabilidad-clientes?desde&hasta`; only `clienteId` (no client name) |
| Dashboard pendientes-cobro | AVAILABLE | `GET /api/dashboard/pendientes-cobro` |
| Digitalizar factura (multipart) | AVAILABLE | `POST /api/gastos/digitalizar`, part `archivo`; returns a `GastoResponse` (whether it is persisted or only a preview is UNCLEAR) |

Other things noticed:
- `GastoResponse` has **no `total`** (only `baseImponible` and `iva`), unlike `IngresoResponse`.
- `GastoResponse.estado` and `IngresoResponse.estadoCobro` are free strings: the possible values are
  not in the schema.
- Responses reference clients only by `clienteId`; there is no embedded client name, so lists need
  a join with `GET /api/clientes`.
- `GET /api/health` exists (not in the planned screens; useful for a "server unreachable" check).
- No endpoint to update the user profile or change the password.

## 6. Decisions for the owner

1. **Money representation** (still pending): the API sends plain JSON `number`s with no declared
   scale. Decide the client type before building gastos, bearing in mind the JSON parser will see a
   number, not a string.
2. **Gastos edit / review / delete are missing.** Either the backend adds them, or those screens are
   dropped or reduced to list/create/detail.
3. **Ingresos edit is missing** — same decision.
4. **Pending-ingresos filter**: use `pendientes-cobro` from the dashboard, filter client-side on
   `estadoCobro`, or ask for a backend filter parameter (needs the `estadoCobro` values documented).
5. **Category assignment for a gasto** is unclear: ask how `categoriaId` is set (server-side from
   digitalizar? a missing field in `CrearGastoRequest`?).
6. **`GastoResponse` lacks `total`**: confirm whether it is intentional (derive in client = breaks the
   "no arithmetic on amounts" rule) or a missing field.
7. **Response nullability**: ask the backend to declare `required`/nullable on responses, or verify
   real responses and encode defaults in the DTOs.
8. **Document public endpoints** (`security: []` on `/api/auth/*` and `/api/health`) and error
   responses (400/401/404/409/429 with body schema) in the OpenAPI.
9. ~~Verify 401 vs 403 for unauthenticated requests~~ — resolved: 401 (see section 4). Still worth
   confirming once against the running backend.
10. Clientes: inactive clientes in the list and a reactivate action are undocumented.
