# ADR-006: Price Snapshot at Order Confirmation

## Status
Accepted

## Context
Prices and rules can change (admin edits) between when a customer configures a pizza and when they actually check out — potentially minutes or hours later if the basket sits idle. An Order must not silently re-price or re-validate against whatever rules/prices happen to be active at click-time, and must never let an admin price change retroactively alter an already-placed Order.

## Decision
A `BasketItem` is a full immutable snapshot at the moment it's added to the basket — not just the price, but the pizza, size, dough, modifications, and the rule/price *version* that produced them (agent.md §7.6). `POST /api/v1/orders` creates the Order only from an already-priced, already-snapshotted basket: it never re-validates or re-prices at checkout time. The source `ConfigurationSession` can still be edited after being added to the basket, so `orders`/`basket` must never re-read it — the snapshot is authoritative from the moment it's taken.

## Consequences
- An admin changing a price or rule never retroactively changes an in-flight basket or a placed Order — what the customer saw is what they pay.
- The basket, not the configuration session, is the checkout boundary: editing a configuration after adding it to the basket has no effect on the already-added snapshot (a deliberate, documented behavior, not an oversight).
- Idempotent order creation (`Idempotency-Key`, agent.md §15.1) is simpler to reason about, since the Order's contents are fully determined by the basket snapshot alone, not by re-running pricing/validation logic that could itself be non-deterministic across retries.
