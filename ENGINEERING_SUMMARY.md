# Engineering Summary

## Requirement interpretation and ambiguity resolutions

The service is implemented as a single Spring Boot application with explicit packages for configuration, domain, persistence, DTOs, services, cache, rate limiting, controllers, and exceptions. The create endpoint is deliberately `/api/v1/urls/create`, not the collection root. Only POST requests under `/api/**` are rate-limited; redirects and read endpoints are not throttled. Analytics reads bypass the redirect cache to guarantee fresh click counts. A cached inactive/expired entry is treated as gone and evicted. A cached entry whose atomic click update affects zero rows is evicted and treated as not found, covering deletion/race scenarios.

The request requirement that Jackson not be imported is honored. Application DTO conversion is left to Spring Boot's configured HTTP message conversion; the rate-limit filter writes its 429 JSON response manually and imports no `com.fasterxml.jackson.*` classes.

## Task decomposition

1. Build and configuration: Maven coordinates, Java level, dependencies, application/test properties.
2. Persistence model: `ShortUrl` entity and repository atomic increment query.
3. Guardrails: URL validation and cryptographically random Base62 code generation.
4. Performance controls: bounded LRU redirect cache and token-bucket POST rate limiting.
5. Core behavior: creation, TTL handling, cache-first resolution, exact click counting, direct analytics reads.
6. HTTP layer: create/read/analytics API, redirect endpoint, structured exception responses.
7. Verification: focused unit tests plus full-context MockMvc integration tests.
8. Documentation and Postman collection.

Dependencies flow from configuration/domain -> repository/guardrails -> cache/rate limit -> service -> controllers -> integration tests.

## AI-assisted execution traceability

| Area | Action | Rationale |
|---|---|---|
| Project skeleton | Generated | Specification fully defines package/build layout |
| Entity/repository | Generated | Direct mapping from required fields and atomic update contract |
| URL validator | Generated then test-driven | Security-sensitive parsing and scheme/host restrictions |
| Code generator | Generated | SecureRandom + Base62 meets anti-enumeration requirement |
| Cache | Generated then behavior-tested | Custom LRU avoids adding blocked dependencies |
| Service | Generated then behavior-tested | Centralizes consistency, TTL, cache, and click semantics |
| Rate limit | Generated | Manual JSON avoids forbidden Jackson imports |
| HTTP controllers | Generated | Exact endpoint/status behavior specified |
| Tests | Generated and executed | Validate requested positive/negative paths |
| Caffeine dependency | Rejected | Requirement explicitly calls for dependency-free LRU and notes download constraints |
| Sequential IDs | Rejected | Easier enumeration and contradicts requested design |
| Async click aggregation | Rejected | Would weaken immediate analytics accuracy |
| TestRestTemplate | Rejected | Not guaranteed on the requested test classpath |

## Risks and validation

Primary risks are framework-version API drift, persistence/query behavior, cache correctness under access-order mutation, and integration serialization. These are addressed by compiling under the requested parent and running the full Maven lifecycle, including Spring context startup and MockMvc integration tests. Security-sensitive URL validation has dedicated negative tests. Cache LRU ordering has a focused survival/eviction test.

## Assumptions

- Forwarded IP headers are trusted at the deployment edge; in an internet-facing deployment they should only be accepted from trusted proxies.
- Custom aliases are globally unique and case-sensitive as persisted by H2/JPA behavior.
- Expiration uses server UTC instants and a link expires when `expiresAt <= now`.
- Redirect targets are accepted after scheme/host validation; SSRF-safe outbound fetching is irrelevant because the service never fetches the destination.

## Limitations

Authentication, authorization, per-user quotas, deletion/deactivation APIs, distributed cache/rate limiting, bucket cleanup, database migrations, observability dashboards, and production database tuning are not included. For multi-node deployment, use PostgreSQL plus Redis-backed caching/rate limiting; for a single-JVM cache upgrade, Caffeine W-TinyLFU is preferred.
