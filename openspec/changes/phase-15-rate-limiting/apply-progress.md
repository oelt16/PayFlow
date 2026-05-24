# Apply Progress: Phase 15 — Rate Limiting

> **Source**: Engram observation #39

## Tasks Completed: 24/24

### Phase 1 (7/7)
- ✅ 1.1–1.3 Dependencies (bucket4j-core, caffeine)
- ✅ 1.4–1.7 Config + properties classes

### Phase 2 (2/2)
- ✅ 2.1 BucketRegistry — Caffeine + Bucket4j
- ✅ 2.2 429 response helper

### Phase 3 (3/3)
- ✅ 3.1 RateLimitFilter at order +15
- ✅ 3.2 Endpoint-specific limits
- ✅ 3.3 429 response with headers

### Phase 4 (6/6)
- ✅ 4.1–4.6 All 6 test classes for payment-service

### Phase 5 (6/6)
- ✅ 5.1–5.6 All files mirrored to merchant-service

## Key Implementation Notes
- **Bean wiring fix**: Removed `@Component` from filter, used `FilterRegistrationBean` in `RateLimitConfig`
- **Tests**: 39 total (18 payment-service + 17 merchant-service + 4 structural)
- **Spec compliance**: 16/16 scenarios
