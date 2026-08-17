-- Not anticipated in V001 (agent.md §7.10 admin module lands in Phase 11)
-- — created here instead, matching V013's recommendation-schema precedent.
CREATE SCHEMA IF NOT EXISTS admin;

CREATE TABLE admin.app_link_setting (
    app_link_id  UUID PRIMARY KEY,
    platform      VARCHAR(20) NOT NULL,
    audience       VARCHAR(20) NOT NULL,
    url              VARCHAR(2000) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT true,
    updated_by         VARCHAR(100),
    updated_at           TIMESTAMPTZ NOT NULL,
    UNIQUE (platform, audience)
);

CREATE TABLE admin.audit_event (
    event_id       UUID PRIMARY KEY,
    timestamp       TIMESTAMPTZ NOT NULL,
    actor_id         VARCHAR(100) NOT NULL,
    actor_role        VARCHAR(20) NOT NULL,
    action             VARCHAR(100) NOT NULL,
    entity_type          VARCHAR(100) NOT NULL,
    entity_id             VARCHAR(100) NOT NULL,
    before_json            TEXT,
    after_json               TEXT,
    correlation_id            VARCHAR(100) NOT NULL
);

CREATE INDEX idx_audit_event_timestamp ON admin.audit_event (timestamp);
