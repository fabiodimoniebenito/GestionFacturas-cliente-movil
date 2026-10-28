# API contract

Source: `GET http://localhost:8080/v3/api-docs` (OpenAPI 3.1.0, `info.version` = `v0`), fetched
2026-10-28 (previous audit: 2026-10-27). Field names and types below are copied from that
document. This file is the source of truth for DTO shapes in this client and **must be updated
whenever the backend changes**. The backend lives in another repository (`GestionFacturas-api`);
this client only documents it.

## Changes since last audit (2026-10-27 -> 2026-10-28)

`docs/openapi.json` (pretty-printed copy of the fetched document) did not exist before, so there
was no file diff; this audit is a first audit of that file: every path, operation, schema, field,
type, `required` list and enum in it was compared against this contract.

**Result: no API changes.** Endpoints, request/response fields, types, nullability (still none
declared) and enum values are identical to what this file already documented.

- Paths: 18; operations: **25**; schemas: 22.
- **Correction (documentation only):** the previous section said "26 operations (was 24)". The
  document has 25 (the endpoint table in section 1 lists all 25). The two added endpoints
  (`PUT /api/gastos/{id}`, `POST /api/gastos/{id}/revisar`) were real; the total was miscounted.
- Money fields: all still `"type": "number"` with no `format`.
- Still true: global `security: [bearerAuth]` with no per-operation override, only `200`
  documented, no `required` list on any response schema, no `nullable`, `GastoResponse` has no
  `total`, `revertirCobro` is `DELETE /api/ingresos/{id}/cobro`.
- Error handling: unchanged. Only "401 on missing/invalid token" is VERIFIED (2026-10-27); the
  expired-token 401 and everything in the status table of section 4 stay NOT verified.

## Previous audit (2026-10-19 -> 2026-10-27)

26 operations (was 24; see the correction above), 22 schemas (was 21). Compared against the schema, field by field.

**Added endpoints**

| Method | Path | operationId | Request | Response (200) |
|---|---|---|---|---|
| PUT | `/api/gastos/{id}` | `editar` | `EditarGastoRequest` (body required), path `id` int64 | `GastoResponse` |
| POST | `/api/gastos/{id}/revisar` | `revisar` | no body, path `id` int64 | `GastoResponse` |

**Removed endpoints:** none.

**Added schema:** `EditarGastoRequest` — required: `total`.

| Field | Type | Constraints |
|---|---|---|
| emisor | string | optional |
| fechaEmision | string, format `date` | optional |
| baseImponible | number (no format) | minimum 0, optional |
| iva | number (no format) | minimum 0, optional |
| total | number (no format) | minimum 0, **required** |
| moneda | string | optional |
| clienteId | integer int64 | optional |
| categoriaId | integer int64 | optional (not in `CrearGastoRequest`) |
| deducible | boolean | optional (not in `CrearGastoRequest`) |

**Changed enum values** (both were plain `string` without enum before):

| Field | Now declared as |
|---|---|
| `GastoResponse.estado` | `string`, enum `BORRADOR`, `PROCESADA`, `REVISADA`, `EXPORTADA` |
| `IngresoResponse.estadoCobro` | `string`, enum `PENDIENTE`, `COBRADA` |

**Changed fields (name / type / nullability) on existing schemas:** none. All other request and
response schemas are identical to the previous audit. Still true: no `required` list on any
response schema, no `nullable`, only `200` documented, `GastoResponse` has no `total`, no
`security: []` override on any operation.

**Not in the schema, observed on the running backend (2026-10-27):** unauthenticated and
invalid-token requests to protected GETs return `401` with header `WWW-Authenticate: Bearer` and
JSON body `{"error":"No autenticado. Inicia sesión de nuevo."}` (see section 4).

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
- Only two enumerations are declared, both on responses: `GastoResponse.estado` and
  `IngresoResponse.estadoCobro` (`string` with `enum`, values in "Changes since last audit"). The
  schema does not say which transitions are allowed.

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
| PUT | `/api/gastos/{id}` (operationId `editar`) | B | `EditarGastoRequest` | path `id` int64 | `GastoResponse` |
| POST | `/api/gastos/{id}/revisar` (operationId `revisar`) | B | – | path `id` int64 | `GastoResponse` |
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

There are no other paths. In particular there is no `DELETE` on `/api/gastos/{id}` and no
`PUT`/`DELETE` on `/api/ingresos/{id}`.

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

`EditarGastoRequest` — required: `total` (fields in "Changes since last audit", same as
`CrearGastoRequest` plus `categoriaId` int64 and `deducible` boolean)

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
`deducible` boolean, `estado` string (enum `BORRADOR`, `PROCESADA`, `REVISADA`, `EXPORTADA`),
`creadoEn` date-time.
**There is no `total` field in `GastoResponse`**, although `CrearGastoRequest` has one.

`IngresoResponse`: `id` int64, `usuarioId` int64, `clienteId` int64, `concepto` string,
`fechaEmision` date, `baseImponible` number, `iva` number, `total` number, `moneda` string,
`estadoCobro` string (enum `PENDIENTE`, `COBRADA`), `fechaCobro` date, `creadoEn` date-time.

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
| `EditarGastoRequest` | `baseImponible`, `iva`, `total` (minimum 0) |
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

The OpenAPI declares no error responses (only `200`), no error schema and no `Retry-After` header.

**VERIFIED against the running backend (2026-10-27, `curl`, no data created):** protected
endpoints return **401** (not 403) when the token is missing or invalid. Tested with no
`Authorization` header and with `Authorization: Bearer garbage` on `GET /api/gastos`,
`/api/clientes`, `/api/ingresos`, `/api/categorias`, `/api/dashboard/pendientes-cobro` and
`/api/dashboard/resumen?desde&hasta`: all 12 calls answered 401, with header
`WWW-Authenticate: Bearer` and body `{"error":"No autenticado. Inicia sesión de nuevo."}`.
**NOT yet verified:** the 401 for an *expired* token and the client's refresh-and-retry on it
(needs a real login; pending a manual run of `./gradlew :desktopApp:run` with an expired access
token). Until then that part rests on the backend source (`HttpStatusEntryPoint(UNAUTHORIZED)`).

Everything below is **per backend convention, NOT verified**:

| Status | Meaning (per `CLAUDE.md`) |
|---|---|
| 400 | Validation or domain error |
| 401 | Bad credentials (login) |
| 404 | Not found (also used for another user's resource) |
| 409 | Conflict: duplicate email/NIF, invalid cobro transition |
| 429 | Rate limited, with `Retry-After` |

- Error bodies are JSON. Domain errors use an `error` message field; validation errors are a map
  of field -> message. (Only the 401 body above has been seen.)
- Rate limits: login 10/min per IP, register 5/hour per IP, everything else 50/min per user.

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
| Gastos edit | AVAILABLE | `PUT /api/gastos/{id}` with `EditarGastoRequest` (`total` required; also `categoriaId`, `deducible`). Whether editing a `REVISADA`/`EXPORTADA` gasto is allowed is UNCLEAR |
| Gastos review | AVAILABLE | `POST /api/gastos/{id}/revisar`, no body, returns `GastoResponse`. Allowed source states and idempotency are UNCLEAR (`estado` enum: `BORRADOR`, `PROCESADA`, `REVISADA`, `EXPORTADA`) |
| Gastos delete | MISSING | No `DELETE` on `/api/gastos/{id}` |
| Ingresos list | AVAILABLE | `GET /api/ingresos` |
| Ingresos list with pending filter | MISSING | List takes no parameters. Workarounds exist but are not a filter: `GET /api/dashboard/pendientes-cobro` (separate, slimmer shape) or filter client-side on `estadoCobro` (now declared: `PENDIENTE`, `COBRADA`) |
| Ingresos create | AVAILABLE | `POST /api/ingresos` (`total` required) |
| Ingresos detail | AVAILABLE | `GET /api/ingresos/{id}` |
| Ingresos edit | MISSING | No `PUT`/`PATCH` on `/api/ingresos/{id}` |
| Ingresos registrar cobro | AVAILABLE | `POST /api/ingresos/{id}/cobro` with optional `fechaCobro` |
| Ingresos revertir cobro | AVAILABLE | `DELETE /api/ingresos/{id}/cobro` |
| Categorias list | AVAILABLE | `GET /api/categorias` |
| Category assigned to a gasto | AVAILABLE (on edit only) | `EditarGastoRequest` has `categoriaId` and `deducible`; `CrearGastoRequest` still has neither, so a new gasto cannot be created with a category. Whether the server assigns one by default (e.g. in digitalizar) is UNCLEAR |
| Dashboard resumen | AVAILABLE | `GET /api/dashboard/resumen?desde&hasta` (both required) |
| Dashboard rentabilidad-clientes | AVAILABLE | `GET /api/dashboard/rentabilidad-clientes?desde&hasta`; only `clienteId` (no client name) |
| Dashboard pendientes-cobro | AVAILABLE | `GET /api/dashboard/pendientes-cobro` |
| Digitalizar factura (multipart) | AVAILABLE | `POST /api/gastos/digitalizar`, part `archivo`; returns a `GastoResponse` (whether it is persisted or only a preview is UNCLEAR) |

Other things noticed:
- `GastoResponse` has **no `total`** (only `baseImponible` and `iva`), unlike `IngresoResponse`.
- `GastoResponse.estado` and `IngresoResponse.estadoCobro` are now declared enums (see top). The
  allowed transitions are not in the schema.
- Responses reference clients only by `clienteId`; there is no embedded client name, so lists need
  a join with `GET /api/clientes`.
- `GET /api/health` exists (not in the planned screens; useful for a "server unreachable" check).
- No endpoint to update the user profile or change the password.

## 6. Decisions for the owner

1. **Money representation** (still pending): the API sends plain JSON `number`s with no declared
   scale. Decide the client type before building gastos, bearing in mind the JSON parser will see a
   number, not a string.
2. **Gastos delete is missing.** The backend adds it, or the gastos screen has no delete.
   (Edit and review now exist.)
3. **Ingresos edit is missing** — same decision.
4. **Pending-ingresos filter**: use `pendientes-cobro` from the dashboard, filter client-side on
   `estadoCobro` (`PENDIENTE`), or ask for a backend filter parameter.
5. **Category on create**: `CrearGastoRequest` has no `categoriaId`. Ask whether to create then
   `PUT` the category (two calls) or add the field to the create request.
6. **`GastoResponse` lacks `total`** (but `EditarGastoRequest`/`CrearGastoRequest` require it): confirm
   whether it is intentional (deriving it in the client breaks the "no arithmetic on amounts" rule)
   or a missing field. It matters now that edit exists: the form cannot be pre-filled with `total`.
7. **Gasto state rules**: which `estado` transitions are allowed, whether `revisar` is idempotent, and
   whether a `REVISADA`/`EXPORTADA` gasto can be edited. The client must not guess these.
8. **Response nullability**: ask the backend to declare `required`/nullable on responses, or verify
   real responses and encode defaults in the DTOs.
9. **Document public endpoints** (`security: []` on `/api/auth/*` and `/api/health`) and error
   responses (400/401/404/409/429 with body schema) in the OpenAPI.
10. Clientes: inactive clientes in the list and a reactivate action are undocumented.
11. Verify the expired-token 401 + refresh against the running backend (section 4).
