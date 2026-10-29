---
name: refrescar-contrato
description: Refresh docs/api-contract.md from the running backend using an exact diff of its OpenAPI document
disable-model-invocation: true
---
Refresh the API contract. Read CLAUDE.md first. No app code, no backend changes.

1. Fetch http://localhost:8080/v3/api-docs (public endpoint only; no protected endpoints, no data 
   creation; if unreachable, STOP and tell me). Save it pretty-printed (jq, or any tool available) 
   to docs/openapi.json, keeping the previous version for the diff.
2. Run `git diff docs/openapi.json`. If there are no differences, report "no changes" and STOP 
   without touching any file. If docs/openapi.json did not exist, treat this as a first audit and 
   regenerate the whole contract.
3. Using ONLY that diff and the schema (never memory), add a "Changes since last audit" section 
   at the top of docs/api-contract.md (endpoints, fields, nullability, enum values) and update the 
   affected sections with exact names and types. For money fields record only the declared JSON 
   type.
4. Update the "Gaps vs planned screens" table (AVAILABLE/MISSING/UNCLEAR, never invent endpoints) 
   and "Decisions for the owner".
5. Error handling: mark as VERIFIED only "401 on missing/expired token"; the rest stays "per 
   backend convention, NOT verified".
6. Check CLAUDE.md's backend contract section against the contract (revert cobro must be 
   DELETE /api/ingresos/{id}/cobro). Fix and list mismatches; otherwise leave it untouched.

Branch docs/refresh-api-contract from main; commit "docs: refresh API contract" including 
docs/openapi.json; no push or merge. Summary: the changes section and the gaps table.
