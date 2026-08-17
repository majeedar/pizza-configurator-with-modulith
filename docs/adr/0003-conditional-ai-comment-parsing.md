# ADR-003: Conditional AI Comment Parsing

## Status
Accepted

## Context
Customers can add a free-text comment to a configuration (e.g. "extra crispy, no basil please"). Parsing this into structured selections (removed ingredients, extras, size/dough hints) needs either a hand-built NLP pipeline or an LLM. Whatever handles it must not become a hard dependency for the majority of orders, which have no comment at all.

## Decision
A non-blank comment routes through the AI Adapter module's `CommentInterpreter` (Deepseek primary, OpenAI fallback — ADR-008); a blank comment skips AI entirely. The AI Adapter never decides validity or price — its output is merged with the structured UI selections and re-validated through the same deterministic Rule module as any other configuration (ADR-002). Any ambiguity or failure (timeout, HTTP error, malformed response, unknown ingredient, or text the model itself couldn't resolve) is honestly surfaced as `unresolvedText`, which the Configuration module turns into `PENDING_REVIEW` rather than guessing.

The prompt explicitly lists only the pizza's actual allowed ingredient/size/dough codes and frames the customer's comment as data to interpret, never as instructions to follow — deliberate prompt-injection resistance, since the comment is untrusted user input reaching a model with (indirect) purchasing-adjacent context.

## Consequences
- The no-comment order flow (the majority case) never touches Deepseek/OpenAI — no latency, cost, or availability dependency for it.
- A confused or malicious comment degrades to `PENDING_REVIEW` (kitchen triage via the Recommendation module, ADR-007) instead of silently producing a wrong configuration or an outright failure.
- Both providers are optional at the infrastructure level (blank API key = stubbed, matching the `agent.md` §27 local-default pattern) — the app is fully functional in an environment with zero AI credentials configured.
