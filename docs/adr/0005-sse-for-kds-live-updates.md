# ADR-005: SSE for KDS Live Updates

## Status
Accepted

## Context
Considered: plain polling, WebSockets, and Server-Sent Events (SSE) for pushing production-board updates to the Kitchen Display System (staff web + native kitchen Android app).

## Decision
Server-Sent Events (`GET /api/v1/kitchen/stream`), not WebSockets. The KDS only ever needs one-directional server→client notifications ("something changed, go refetch") — it never needs to push data back over the same channel (actions like approve/start/ready/complete go over ordinary REST calls). SSE is plain HTTP, works through the same auth/reverse-proxy path as everything else, and needs no separate protocol upgrade handling.

Critically, per agent.md §17, **SSE here is an optimization, not the source of truth**: the backend listens `AFTER_COMMIT` on the relevant `orders` domain events before emitting, and every client (staff web, native kitchen Android) treats every event identically — "refetch `GET /api/v1/kitchen/orders`" — never trusting event payload data directly, and reconnects on any drop with a fixed backoff. A client that reconnects after being offline for a while is always consistent, since it just refetches the full board.

Both the staff web app and the native kitchen Android app use a hand-rolled streaming client (`fetch`+`ReadableStream` on web, OkHttp `BufferedSource` on Android) instead of the browser's native `EventSource` API, because `EventSource` cannot attach an `Authorization` header and the stream is gated by the same staff bearer JWT as every other `/api/v1/kitchen/**` endpoint.

## Consequences
- No WebSocket infrastructure (no separate ping/pong keep-alive, no sticky-session concern behind nginx) to operate.
- The "refetch on any event" design means a missed or duplicate event is harmless — nothing has ever needed to rely on SSE for correctness, only for latency.
- The custom streaming client (instead of `EventSource`) was the direct, deliberate consequence of choosing bearer-JWT auth (ADR/§14.1) over cookie-based sessions.
