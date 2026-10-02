# Engineering Scenarios

## Greenfield scenario — TTL expiry

### Decomposition
1. Accept optional `ttlSeconds` in the create request.
2. Reject zero/negative values at the service boundary.
3. Persist `expiresAt = createdAt + ttl`.
4. Check expiry before redirects from either cache or database.
5. Return HTTP 410 for expired links and evict stale cache entries.

### Execution
`UrlShortenerService.create` calculates the expiry instant. `ShortUrl.isExpired(now)` owns the time comparison. `resolveAndRecordClick` checks both cached and database-backed values before counting a click.

### Validation
Unit tests cover positive TTL creation, invalid TTL rejection, and expired-link resolution. Integration behavior is covered indirectly through the same service in the full Spring context.

## Brownfield scenario — rate limiting

### Decomposition
1. Avoid changing redirect/read behavior.
2. Insert a servlet filter early in the chain.
3. Scope filtering to POST `/api/**` only.
4. Key buckets by first `X-Forwarded-For` hop, otherwise remote address.
5. Use linear refill and return a dependency-free JSON 429 response.

### Execution
`RateLimitFilter` owns a concurrent per-key map of `TokenBucket` instances. Each bucket synchronizes token refill and consumption. No external cache/library was added.

### Validation
The filter is loaded by the integration context, while high test limits prevent unrelated test flakiness. The implementation remains isolated enough to unit-test independently if rate policy expands.

## Ambiguous scenario — analytics freshness

### Decomposition
1. Clarify whether analytics may be eventually consistent with redirect cache state.
2. Preserve cache benefits only for redirect destination lookup.
3. Keep click increments synchronous and database-side.
4. Read analytics directly from the database.

### Execution
The redirect cache stores destination, expiry, and active state only. Every successful redirect invokes the repository `UPDATE` that increments `clickCount` and writes `lastAccessedAt`. `getByCode` bypasses the cache.

### Validation
The service test verifies two resolutions perform one entity lookup but two click updates. The integration flow creates a code, redirects once, and confirms analytics reports `clickCount == 1`.
