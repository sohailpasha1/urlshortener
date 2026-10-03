# Engineering Summary

## 1. Requirement Understanding

### Stated intent

Build a runnable URL shortener that demonstrates production-oriented engineering choices rather than only happy-path
CRUD: concurrency-safe code creation, bounded redirect caching, abuse controls, expiry, click analytics, structured
errors, SSRF guardrails, pluggable code generation, and automated verification.

### Normalized problem

The system has two latency-sensitive paths with different consistency needs:

1. **Create path:** validate an externally supplied URL, select a code, persist a unique mapping, and return a stable
   public URL.
2. **Redirect path:** resolve a short code quickly, reject inactive/expired mappings, record a click, and issue a 302.

The design therefore separates validation, strategy selection, persistence isolation, cache lookup, and request
filtering while keeping the public API small.

### Ambiguities + resolutions

| Topic                | Ambiguity                                                                               | Resolution                                                                                                                           |
|----------------------|-----------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Analytics scope      | "Analytics" could mean user-agent, referrer, geography, time series, etc.               | Narrowed to click count + last-accessed timestamp, with URL identity/created/expiry state for context.                               |
| Persistence choice   | Requirements need durable sequence state but only prescribe H2 locally.                 | Use file H2 by default, standard JPA repositories, and a single durable `code_sequence` row.                                         |
| Reliability scope    | "Production-grade" can imply distributed infrastructure.                                | Implement bounded single-instance cache/limiter and document Redis/distributed migration as the next production step.                |
| Code scheme          | Random codes and deterministic counter-based codes have different collision properties. | Keep a `ShortCodeStrategy`; random uses SecureRandom and retries; Feistel uses a durable monotonic counter and permutation.          |
| Uniqueness guarantee | Random generation cannot prove uniqueness before insertion.                             | Rely on a unique DB constraint plus isolated retry attempts; Feistel is unique by construction until its finite domain is exhausted. |

## 2. Task Decomposition

The implementation was sequenced to minimize rework and isolate dependencies:

1. Dependencies and Maven compiler/annotation processing.
2. Typed configuration and environment defaults.
3. Domain entities and repositories.
4. Pluggable short-code strategy, including durable allocation and Feistel codec.
5. URL validator and SSRF policy.
6. Core `UrlShortenerService` and expiry semantics.
7. `REQUIRES_NEW` write isolation.
8. Redirect cache.
9. API and redirect controllers.
10. Global exception handler and error envelope.
11. Rate-limit filter and bounded client state.
12. Unit, controller, integration and context tests.
13. README, engineering traceability, scenarios, Postman and repository hygiene.

## 3. Codebase Reasoning (Brownfield Baseline)

The starting point was treated as a minimal webmvc/Lombok-style scaffold rather than assuming a mature application
architecture. The impact analysis was therefore deliberately additive:

- Preserve the base package `com.urlshortener` and existing Spring Boot entry point.
- Add JPA, validation and actuator without introducing a second application module.
- Keep HTTP contracts in records so validation and response shapes are explicit.
- Introduce `service`, `repository`, `domain`, `cache`, `ratelimit`, `config` and `exception` packages instead of
  putting behavior into controllers.
- Use constructor injection throughout, with framework-managed components selected by configuration.
- Keep test infrastructure MockMvc-based so controller contracts can be tested without a live HTTP server.

This minimizes framework surface area while leaving clear seams for later PostgreSQL/Redis migration.

## 4. AI-Assisted Execution — Traceability

| Artifact                  | AI role                                               | Engineer action/rationale                                                                                                                     |
|---------------------------|-------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| Maven build               | Generated dependency/plugin baseline                  | Checked exact Spring Boot 4.1.1 parent, Java 21, required starters and Lombok processor paths.                                                |
| DTO records               | Drafted API contracts                                 | Reviewed validation annotations and response field order/semantics against the requested contract.                                            |
| JPA entities/repositories | Generated persistence layer                           | Preserved unique short-code constraint, atomic click update and pessimistic sequence locking.                                                 |
| Random strategy           | Generated SecureRandom Base62 implementation          | Kept it non-enumerable and configurable rather than replacing it with sequential IDs.                                                         |
| Feistel strategy          | Generated codec/allocator design                      | Edited intent from plain counter exposure to keyed Feistel permutation with fixed-width Base62 output.                                        |
| URL validator             | Drafted SSRF rules                                    | Explicitly retained IP-literal classification only, avoiding DNS resolution/TOCTOU.                                                           |
| Write isolation           | Proposed collision handling                           | Enforced `REQUIRES_NEW` around each insert attempt so duplicate-key rollback is isolated.                                                     |
| Redirect cache            | Generated LRU implementation                          | Kept cache bounded and synchronized; click persistence remains authoritative.                                                                 |
| Rate-limit filter         | Initial AI output used Jackson                        | **Rejected:** filter response was changed to hand-built JSON because the contract explicitly disallowed relying on Jackson for filter output. |
| Controller tests          | Initial AI output considered `TestRestTemplate`       | **Rejected:** project uses MockMvc because `TestRestTemplate` is not part of the selected test starter.                                       |
| H2 configuration          | AI proposed `AUTO_SERVER`/`DB_CLOSE_ON_EXIT` variants | **Rejected:** simplified to the requested file URL with explicit H2 dialect and normal application lifecycle.                                 |
| Controllers               | Drafted REST/redirect mappings                        | Verified exact paths, response codes, regex short-code mapping and `Cache-Control: no-store`.                                                 |
| Test suite                | Generated unit/integration coverage                   | Added dedicated controller tests and corrected the invalid standalone regex assertion after it produced a false 500.                          |
| Documentation             | AI drafted technical narrative                        | Engineer review aligned claims with actual code and separated implemented behavior from deferred production work.                             |

### Quality gates and secure AI usage

- Treat the prompt as a contract, not as permission to add unrelated infrastructure.
- Prefer deterministic tests over prose claims.
- Reject generated approaches that violate explicit dependency or security constraints.
- Review SSRF, concurrency, transaction boundaries, trust headers, and error serialization manually.
- Keep secrets, credentials, and private production data out of AI prompts and test fixtures.
- Validate generated code with Maven/JUnit, static inspection, and reproducible local configuration.

## 5. Risks, Trade-offs & Validation

| Risk                                   | Mitigation                                                        | Validation                                                       |
|----------------------------------------|-------------------------------------------------------------------|------------------------------------------------------------------|
| SSRF to local services                 | Reject localhost and local/private IP literals; no DNS resolution | `UrlValidatorTest`                                               |
| Random code collision                  | DB uniqueness + five isolated retry attempts                      | `UrlShortenerServiceTest.collisionRetries`, `fiveCollisionsFail` |
| Feistel duplicate codes                | Bijective permutation over finite domain                          | `FeistelCodecTest`, `FeistelCounterStrategyTest`                 |
| Counter races                          | Pessimistic row lock + durable block reservation                  | `SequenceBlockAllocatorTest`                                     |
| Duplicate insert poisoning transaction | `ShortUrlWriter` uses `REQUIRES_NEW`                              | Service/integration collision tests                              |
| Cache stampede/incorrect state         | Thread-safe bounded LRU; validate active/expiry on hits           | `ShortUrlCacheTest`, `UrlShortenerServiceTest`                   |
| Unlimited limiter memory               | Maximum clients + idle eviction                                   | `RateLimitFilterTest`                                            |
| Spoofed client identity                | `X-Forwarded-For` trusted only by explicit config                 | `RateLimitFilterTest`                                            |
| Error contract drift                   | Central `GlobalExceptionHandler` and controller tests             | `UrlShortenerControllerTest`, `UrlShortenerIntegrationTest`      |
| Redirect caching                       | `Cache-Control: no-store`                                         | `RedirectControllerTest`, integration redirect test              |
| Runtime/test DB contamination          | File H2 only in main config; in-memory `create-drop` in tests     | `src/test/resources/application.properties`, integration suite   |

## 6. Assumptions

1. Short codes are opaque identifiers; users do not need sequential human-readable IDs.
2. Click counting must be visible immediately after a successful redirect in this prototype.
3. A known short code is sufficient to read its metadata/analytics because authentication is explicitly out of scope.
4. Ordinary hostnames are not DNS-resolved by the URL validator to avoid a validation-time DNS race.
5. The Feistel domain is finite and exhaustion is a configuration/runtime condition that should fail explicitly.
6. H2 is a local/prototype persistence choice, not the final production database.

## 7. Limitations

- Cache and rate limiting are local to one process.
- Click counting adds synchronous database write latency.
- No authentication, authorization, ownership or tenant isolation.
- `ddl-auto=update` is not a production migration strategy.
- SSRF protection does not implement full DNS-aware egress policy or redirect-chain inspection.
- Expired records are not automatically purged.
- No distributed observability/tracing or external metrics backend is included.

## 8. Controlled Oversight

The engineer remains responsible for acceptance criteria, security posture and final correctness. AI is used for
implementation acceleration and test/documentation generation, but generated code is treated as a draft until it
satisfies explicit contracts and automated checks.

The clearest examples are the deliberate corrections: Jackson was removed from filter serialization, `TestRestTemplate`
was avoided in favor of MockMvc, the H2 connection URL was simplified, random generation was retained over exposed
sequential codes, Feistel was used to preserve deterministic uniqueness without exposing the counter, SSRF validation
was narrowed to defensible IP-literal checks, and write attempts were isolated with `REQUIRES_NEW`.
