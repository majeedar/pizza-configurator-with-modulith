# ADR-002: Deterministic Rule Engine as Source of Truth

## Status
Accepted

## Context
A pizza configuration must be validated against business constraints (max quantity of an extra, ingredient availability, allowed size/dough combinations). The AI Adapter module (ADR-003) can *interpret* a free-text comment into structured selections, which raised the question of whether AI output should also be trusted to decide whether a configuration is valid.

## Decision
All validity decisions are made by a deterministic, typed `RuleEvaluator` implementation per rule type (e.g. `MaxQuantityEvaluator`), never by a language model. The Rule module is the only source of truth for `VALID`/`INVALID`/violations — the AI Adapter only ever *proposes* a structured interpretation of a comment, which is then re-validated through the exact same rule pipeline as a no-comment configuration. An AI result is never trusted merely because it parsed successfully.

## Consequences
- Rule outcomes are reproducible, testable with plain unit tests, and don't depend on an external AI provider being available or consistent between calls.
- The standard no-comment order flow never depends on Deepseek/OpenAI (agent.md §28) — AI is strictly additive, never a validity gatekeeper.
- Adding a new rule type means writing a new `RuleEvaluator`, not prompt-engineering — a change that's code-reviewable and unit-testable rather than a prompt tweak with unpredictable blast radius.
