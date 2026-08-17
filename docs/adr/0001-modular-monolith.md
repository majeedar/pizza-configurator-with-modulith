# ADR-001: Modular Monolith instead of Microservices

## Status
Accepted

## Context
The domain splits cleanly into ~13 bounded modules (catalog, rules, pricing, configuration, basket, orders, kitchen, notification, admin, aiadapter, recommendation, security, shared), each with its own persistence schema. A microservices split along the same boundaries was considered, since the boundaries already exist conceptually.

## Decision
Ship a single Spring Boot deployable enforced as a modular monolith via Spring Modulith (`spring-modulith-starter-core`/`-test`), not independently deployable services. Each module gets its own Postgres schema, a restricted `api`/`@NamedInterface` surface, and cross-module coupling only through published interfaces or domain events — the same discipline microservices would force, without the operational cost (service mesh, distributed tracing just to debug a single request, N deployment pipelines, network calls replacing in-process method calls).

`ModularityTests` (Spring Modulith's boundary-verification test) runs on every `mvn verify` and fails the build on a violation — this is the actual enforcement mechanism, not just a convention documented in a wiki. It has caught real violations during development (e.g. `shared` module code reaching into `security.domain.EmployeeRole` directly).

## Consequences
- A single Docker image, a single Compose service, a single JVM process, a single Hetzner VM to operate (§27) — matches the "run on one VM" deployment target from the start.
- Module boundaries are real (compiler- and test-enforced) but a genuine microservices split later is still possible, since each module already owns its own schema and talks to others only through narrow published APIs — the natural extraction seams already exist.
- Cross-module transactions stay simple (same JVM, same connection pool) until a real scaling need forces a split; premature service boundaries were avoided per the general "don't build for hypothetical requirements" principle.
