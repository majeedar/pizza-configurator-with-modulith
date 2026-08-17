-- Not anticipated in V001 (agent.md §7.11 recommendation module lands in
-- Phase 10) — created here instead, matching how other modules' first
-- table migration owns its own schema creation.
CREATE SCHEMA IF NOT EXISTS recommendation;

CREATE TABLE recommendation.review_request (
    review_request_id       UUID PRIMARY KEY,
    configuration_id         UUID NOT NULL,
    status                    VARCHAR(40) NOT NULL,
    reason                     VARCHAR(1000),
    original_request_json      TEXT NOT NULL,
    proposed_modification_json TEXT,
    reviewed_by                 VARCHAR(100),
    reviewed_at                  TIMESTAMPTZ,
    customer_response             VARCHAR(20),
    customer_responded_at          TIMESTAMPTZ,
    created_at                      TIMESTAMPTZ NOT NULL,
    updated_at                       TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_review_request_configuration_id ON recommendation.review_request (configuration_id);
CREATE INDEX idx_review_request_status ON recommendation.review_request (status);
