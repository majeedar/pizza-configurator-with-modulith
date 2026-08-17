# ADR-010: Single Hetzner VM via GitHub Actions SSH Deploy

## Status
Accepted

## Context
A production deployment target was needed for v1. Options considered: a managed Kubernetes cluster, a PaaS (Fly.io/Render/Railway-style), or a single VM. Given ADR-001 (one deployable, modular monolith) and modest expected v1 scale, Kubernetes would add orchestration overhead with no corresponding benefit; a PaaS trades operational simplicity for vendor lock-in and less control over Postgres/nginx/backup specifics the project wants to own directly.

## Decision
A single Hetzner VM, reached via a GitHub Actions CI/CD pipeline (`deploy.yml`) over SSH with a deploy-only key. The pipeline: build immutable images tagged with the Git SHA, scan them (Trivy, container-build.yml/deploy.yml), publish to GHCR, SSH into the VM, `docker compose -f compose.prod.yaml pull && up -d`, then run a smoke test against the public URL. Postgres runs as a container on the same VM (no managed database service on Hetzner), so backups are explicit: scheduled `pg_dump` (systemd timer, `infrastructure/systemd/`) uploaded off-VM, plus a restore procedure that's actually been run against real data (`infrastructure/scripts/{backup,restore}-postgres.sh`), not just written and assumed to work. TLS via nginx + certbot/Let's Encrypt (`infrastructure/nginx/`), one server block per frontend domain plus the API domain. Rollback is "redeploy the previous Git-SHA-tagged image via the same `deploy.yml` workflow" (supported via a `workflow_dispatch` input), not a separate rollback mechanism.

No persistent staging VM for v1 (agent.md §27) — CI's own ephemeral Compose stack (`integration.yml`) serves as pre-release verification instead.

## Consequences
- One VM to patch, monitor, and pay for — the operational surface matches the modular-monolith decision (ADR-001) instead of fighting it with a distributed deployment target.
- Backups are the entire durability story for Postgres (no managed-service safety net) — the restore procedure being tested, not just documented, is load-bearing, not optional polish.
- `deploy.yml` requires real secrets (`HETZNER_HOST`/`HETZNER_SSH_USER`/`HETZNER_SSH_KEY`/`GHCR_TOKEN`) and a real domain that don't exist in every environment this repo is developed in — it's been written and locally validated piece-by-piece (nginx config syntax, backup/restore against real data, the container scan, the E2E suite, the load test) but not executed end-to-end against an actual Hetzner VM as part of this work; see README "Hetzner deployment" for exactly what was and wasn't verified.
- Scaling beyond one VM's capacity is a deliberately deferred problem — revisit only if real usage demands it, not preemptively.
