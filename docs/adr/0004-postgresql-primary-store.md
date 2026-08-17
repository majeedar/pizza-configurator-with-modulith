# ADR-004: PostgreSQL as Primary Store

## Status
Accepted

## Context
Every module needs durable, transactional, relational storage (orders, configurations, catalog, rules, prices, audit log). Options considered: PostgreSQL, MySQL/MariaDB, or a document store (MongoDB) for the more JSON-shaped data (e.g. `RuleDefinition.parameters`, `ReviewRequest.originalRequestJson`).

## Decision
A single PostgreSQL instance, one schema per module, managed by Flyway (`db/migration`, always-applied; `db/dev-migration`, local-profile-only demo seed data). JSON-shaped fields that don't need relational querying (rule parameters, review-request snapshots) are stored as plain `TEXT`/`jsonb` columns rather than justifying a second datastore — Postgres's `jsonb` support covers the "semi-structured data" need without giving up transactional guarantees or introducing a second thing to operate, back up, and reason about consistency for.

## Consequences
- One database engine to operate, back up (§27/§31 — scheduled `pg_dump` + tested restore, since Hetzner has no managed Postgres), and reason about transactionally across module boundaries within the same JVM (ADR-001).
- Schema-per-module (not database-per-module) keeps a single connection pool and transaction manager while still giving each module a clearly namespaced, independently migratable set of tables.
- No polyglot persistence to operate for a v1 with modest scale — matches the "don't optimize/architect prematurely" principle; revisit only if a concrete need (e.g. full-text search at scale, a genuinely document-shaped workload) appears.
