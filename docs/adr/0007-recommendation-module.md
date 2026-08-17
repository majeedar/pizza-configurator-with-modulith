# ADR-007: Recommendation Module for Kitchen Accept/Recommend/Reject

## Status
Accepted

## Context
Some configurations can't be resolved automatically: a rule violation with no deterministic suggestion, or an AI-unresolved comment (ADR-003). Someone has to decide what happens next. The options were: reject outright with no path forward, silently apply a best-guess fix, or route to a human (kitchen staff) for a real decision — and if routed to kitchen, whether that logic belongs inside the `configuration` module itself or a separate module.

## Decision
A separate `recommendation` module owns the kitchen-side triage (`ReviewRequest`) and the customer's response to a kitchen recommendation, kept out of `configuration` to avoid a dependency cycle (`recommendation` already depends on `configuration` to revalidate/reprice; the reverse would create a cycle Spring Modulith's `ModularityTests` would reject). When `configuration` can't resolve a request automatically, it publishes `ConfigurationReviewRequested`; `recommendation` listens `AFTER_COMMIT` (its own `REQUIRES_NEW` transaction) and opens a `ReviewRequest`. Kitchen then **Accepts** (revalidate/reprice as-is), **Recommends** (propose a full alternative, notify the customer), or **Rejects** (terminal). A `RECOMMENDED_BY_KITCHEN` request additionally waits on the customer's own **Accept**/**Reject**.

Every path funnels through the same `ConfigurationReviewIntegration.approveOriginal`/`approveWithPatch`/`reject` published by `configuration` — `recommendation` never revalidates or reprices itself, and `configuration` never needs to know `ReviewRequest` exists at all.

## Consequences
- No Order is ever created by this module, regardless of path — checkout still requires adding the (now `REVIEW_APPROVED`) configuration to a basket and checking out normally (ADR-006 still applies unchanged).
- The `AFTER_COMMIT`/`REQUIRES_NEW` pairing was a real bug caught during development (a `AFTER_COMMIT` listener that tries to persist on the now-tearing-down triggering transaction silently "succeeds" with a generated id that's never actually committed) and was applied proactively here once understood.
- A confused customer comment or an unresolvable rule violation degrades gracefully to a human decision instead of a dead end or a silently-wrong configuration.
