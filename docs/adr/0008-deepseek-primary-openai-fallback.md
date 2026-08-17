# ADR-008: Deepseek Primary with OpenAI Fallback

## Status
Accepted

## Context
Comment interpretation (ADR-003) needs an LLM provider. A single-provider dependency means any outage or rate-limit event on that provider degrades every commented order to `PENDING_REVIEW`, even though the no-comment flow is unaffected.

## Decision
Deepseek is the primary provider, OpenAI the fallback, both behind an in-process circuit breaker (3 consecutive failures opens it for 30s, then a half-open trial request) and a per-provider timeout. On timeout, HTTP error (including `429`), or malformed/schema-violating JSON, `CommentInterpreter` fails over to the next provider; if both fail, it returns a result with non-empty `unresolvedText` rather than throwing. Both `DeepseekClient`/`OpenAiClient` treat a blank API key as "not configured" and never attempt the HTTP call at all — the same "stub when unconfigured" pattern used for notification providers (ADR-011).

## Consequences
- A Deepseek outage alone doesn't take down comment interpretation — it falls through to OpenAI automatically.
- Both providers down simultaneously degrades to `PENDING_REVIEW` (Recommendation module, ADR-007) rather than an error surfaced to the customer.
- No AI credentials configured (the local/dev default) means every commented order goes straight to `PENDING_REVIEW` with zero outbound HTTP calls — fully functional for local development and CI without any API keys.
