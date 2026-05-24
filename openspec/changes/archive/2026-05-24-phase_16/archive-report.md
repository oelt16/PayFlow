# Archive Report: Phase 16 — OpenAPI 3.1 Documentation & Swagger UI

**Date**: 2026-05-24
**Change Name**: phase_16
**Status**: ✅ Complete

---

## Executive Summary

Phase 16 adds OpenAPI 3.1 documentation + Swagger UI across all 3 REST services (payment-service, merchant-service, webhook-service) via springdoc-openapi 2.6.0. This is a **documentation-only change** — zero business logic modifications. The change enables interactive API exploration for onboarding, interview demos, and external auditing, and is a prerequisite for Phase 17 API gateway aggregation.

Each service independently hosts its OpenAPI spec at a per-service prefixed path (`/{service}/v3/api-docs`, `/{service}/swagger-ui.html`), with nginx pass-through routing from port 3000. All 15 public endpoints are documented with `@Tag`/`@Operation`/`@ApiResponse` annotations, ~23 DTOs have `@Schema` annotations with descriptions and examples, and internal endpoints (`/internal/webhooks/dispatch`, `/v1/internal/merchants/validate-key`) are hidden via `@Hidden`. A Bearer JWT auth scheme is declared in each service's OpenAPI config with the "Authorize" button visible in Swagger UI. Typed `ApiErrorResponse` records replace generic `Map.of()` error responses across all 3 services, exposing 4 typed fields (`code`, `message`, `requestId`, `param`). The `Idempotency-Key` header is documented as a parameter on payment POST endpoints.

All 10 spec requirements (R1–R10) pass verification. All 16 implementation tasks are marked complete. All existing tests pass across all 3 backend services (~180 unit tests) plus 27 frontend tests with no regressions.

---

## Implementation Stats

| Metric | Value |
|--------|-------|
| Files changed | 39 |
| Lines added | 955 |
| Lines removed | 642 |
| Net change | +313 lines |
| Tasks completed | 16/16 (100%) |
| New files created | 3 (OpenApiConfig beans) |
| Modified files | 36 |
| Controllers annotated | 5 |
| Public endpoints documented | 15 (7 payment + 4 merchant + 4 webhook) |
| Internal endpoints hidden | 2 |
| DTOs with @Schema | ~23 (9 payment + 7 merchant + 7 webhook) |
| ApiErrorResponse records | 3 (one per service) |
| nginx location blocks | 3 (per-service prefix: /payment/, /merchant/, /webhook/) |
| Springdoc version | 2.6.0 |

---

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Version management | Parent POM property + dependency management | Single source of truth, matches existing pattern (bucket4j, junit) |
| Config bean approach | Per-service `OpenApiConfig` | Each service owns its Info/Security/Server; idiomatic Spring; no shared module needed |
| Internal endpoint gating | `@Hidden` on class | Zero-config, annotation-only; works for 2 internal controllers |
| Error response typing | Per-service `ApiErrorResponse` record | Avoids generic `object` in OpenAPI; no shared Maven module needed despite duplication |
| Doc aggregation | nginx location blocks per service | Direct mapping to existing nginx pattern; Phase 17 gateway not yet built |
| Swagger UI path strategy | `/{service-name}/swagger-ui.html` via nginx pass-through | Clean URL per service; prefix consistent through nginx and direct access; no `proxy_redirect` or `sub_filter` needed |
| Path prefix approach | Per-service prefix (e.g., `:8081/payment/swagger-ui.html`) instead of root (`:8081/swagger-ui.html`) | Prevents conflicts when nginx serves all 3 services; enables consistent pass-through proxy without path stripping |

---

## Verification Results

**Verifier**: sdd-verify agent
**Date**: 2026-05-24

| Requirement | Result |
|:-----------:|:------:|
| R1: `/v3/api-docs` exposes OpenAPI 3.1 per service | ✅ PASS |
| R2: Swagger UI at `/swagger-ui.html` | ✅ PASS |
| R3: Endpoints have @Tag/@Operation/@ApiResponse | ⚠️ WARNING (spec says 17, actual 15 — spec arithmetic error) |
| R4: DTOs have @Schema annotations | ✅ PASS |
| R5: Bearer auth scheme declared | ✅ PASS |
| R6: Internal endpoints hidden with @Hidden | ✅ PASS |
| R7: Typed ApiErrorResponse instead of generic object | ✅ PASS |
| R8: Idempotency-Key header on payment POST endpoints | ✅ PASS |
| R9: nginx routes for swagger-ui + v3/api-docs | ✅ PASS |
| R10: springdoc version in parent POM | ✅ PASS |

**Test Results**:
| Suite | Tests | Result |
|-------|-------|--------|
| payment-service | 158 run (7 skipped) | ✅ BUILD SUCCESS |
| merchant-service | all passed | ✅ BUILD SUCCESS |
| webhook-service | 10 run (1 skipped) | ✅ BUILD SUCCESS |
| frontend | 27 tests, 8 files | ✅ All passed |

**Issues**:
- ⚠️ WARNING: Spec says "17 public endpoints" but actual is 15. Spec arithmetic error — implementation matches tasks (7+4+4=15) and design. No implementation gap.
- Docker-dependent integration tests skipped (Docker Desktop detected but not running) — existing exclusions, not related to Phase 16.

**Design compliance**: All 6 architecture decisions from design.md are correctly implemented.

---

## Delta Spec Sync

**No delta specs to sync.** This change introduced a new capability (`api-documentation`) — there was no existing `openspec/specs/` baseline. The spec lives in the change archive as a full document. If a main `api-documentation` spec is needed in the future, it can be extracted from this change.

---

## Post-Archive Notes

- A user-facing access guide exists at **`PHASE_16_OPEN_API_DOC.md`** in the project root. It documents how to access Swagger UI via nginx (port 3000) and directly per service, CLI health checks, troubleshooting steps, and a full verification checklist. **Keep this guide updated** if nginx routing or Swagger UI paths change.
- Phase 17 (API gateway) can consume these per-service OpenAPI specs for aggregation — the specs are accessible at `/{service}/v3/api-docs` via nginx.
- The `openspec/changes/phase_16/` archive folder contains: proposal, spec, design, tasks, verify-report, and this archive report.

---

## Artifacts

| Artifact | Path |
|----------|------|
| Proposal | `openspec/changes/archive/2026-05-24-phase_16/proposal.md` |
| Spec | `openspec/changes/archive/2026-05-24-phase_16/spec.md` |
| Design | `openspec/changes/archive/2026-05-24-phase_16/design.md` |
| Tasks | `openspec/changes/archive/2026-05-24-phase_16/tasks.md` |
| Verify Report | `openspec/changes/archive/2026-05-24-phase_16/verify-report.md` |
| Archive Report | `openspec/changes/archive/2026-05-24-phase_16/archive-report.md` |
| User Guide | `PHASE_16_OPEN_API_DOC.md` (project root) |
