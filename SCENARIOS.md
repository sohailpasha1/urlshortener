# Worked Engineering Scenarios

## A — Greenfield: Add link expiry (TTL)

### Requirement interpretation

A created link may have an optional TTL. A missing TTL means no expiry; a non-positive TTL is invalid; a TTL above the
configured maximum is invalid. Once expired, the short code must behave as gone (`410`) rather than as an unknown code
(`404`).

### Decomposition

1. Add nullable `expiresAt` to `ShortUrl`.
2. Add `isExpired(Instant now)` as a domain-level predicate.
3. Add `ttlSeconds` to the create request.
4. Resolve expiry centrally in `UrlShortenerService`, with overflow-safe arithmetic.
5. Check active/expiry state in both cache-hit and database-miss redirect paths.
6. Map expiry to `ShortUrlGoneException` and HTTP 410.
7. Cover positive TTL, invalid TTL, cap enforcement and expired redirects.

### AI-assisted execution and corrections

The implementation was initially treated as a service-only concern. Engineer review moved the invariant into the domain
(`ShortUrl.isExpired`) so every caller uses the same UTC `Instant` semantics. The service remains responsible for
interpreting user TTL input and applying the configured maximum.

### Validation

- `UrlShortenerServiceTest.positiveTtlCreatesExpiry`
- `UrlShortenerServiceTest.zeroTtlInvalid`
- `UrlShortenerServiceTest.ttlAboveCapInvalid`
- `UrlShortenerServiceTest.expiredIsGone`
- `UrlShortenerControllerTest.mapsGoneTo410`
- Integration redirect/expiry coverage

## B — Brownfield: Harden against abuse (rate limiting)

### Requirement interpretation

Only create operations under `POST /api/**` should consume rate-limit tokens. Client identity is the remote address
unless `trust-forwarded-for=true`, in which case only the first `X-Forwarded-For` hop is used. Client state must be
bounded and idle entries evicted when the client map grows beyond its configured maximum.

### Decomposition

1. Add typed rate-limit configuration.
2. Implement a synchronized linear-refill `TokenBucket`.
3. Add a servlet `OncePerRequestFilter` with order 1.
4. Scope filtering to `POST /api/**`.
5. Maintain per-client buckets and idle timestamps.
6. Emit `429`, `Retry-After: 1`, and the exact JSON envelope.
7. Test client scoping and forwarded-header trust behavior.

### AI-assisted execution and corrections

The first generated filter response used Jackson serialization. That conflicted with the explicit requirement that
hand-written filter output must not rely on Jackson being present. The engineer replaced it with controlled JSON
construction and explicit `Content-Type`/`Retry-After` headers. The filter remains independent of controller advice
because it executes before controller dispatch.

### Validation

- `RateLimitFilterTest` — filter scope and client-key behavior.
- Integration validation through `UrlShortenerIntegrationTest`.
- Runtime configuration defaults keep `trust-forwarded-for=false`.

## C — Ambiguous: Add analytics

### Requirement interpretation

"Analytics" could encompass a large event model. For this prototype it was narrowed to the minimum operationally useful
facts explicitly supported by the domain: total click count and last access time, plus short-code/original URL and
lifecycle timestamps for context. Referrer, user-agent, geography, unique visitors, time buckets and event retention are
deferred.

### Decomposition

1. Store `clickCount` and nullable `lastAccessedAt` on `ShortUrl`.
2. Expose an `AnalyticsResponse` record.
3. Add `GET /api/v1/urls/{shortCode}/analytics`.
4. Increment count and update last access in one SQL `UPDATE`.
5. Execute the increment in the redirect service transaction.
6. Keep cache lookup independent from analytics response retrieval.
7. Verify analytics after redirect in MockMvc integration tests.

### AI-assisted execution and corrections

AI initially treated analytics as a possible event-streaming feature. Engineer review constrained the first release to
synchronous aggregate counters so the requirement stayed testable and consistent with the existing entity model. The
click update uses one atomic repository update rather than read-modify-write application logic, reducing lost-update
risk under concurrent redirects.

### Validation

- `UrlShortenerServiceTest.resolveCacheMissLoadsAndClicks`
- `UrlShortenerServiceTest.resolveCacheHitClicksWithoutLoad`
- `UrlShortenerControllerTest.analyticsReturnsAnalyticsPayload`
- `UrlShortenerIntegrationTest.redirectAndAnalytics`
- `UrlShortenerIntegrationTest.metadata`
