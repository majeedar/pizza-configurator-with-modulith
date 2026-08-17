CREATE TABLE rules.rule_definition (
    rule_id         UUID PRIMARY KEY,
    rule_code       VARCHAR(100) NOT NULL UNIQUE,
    rule_type       VARCHAR(30) NOT NULL,
    scope_type      VARCHAR(20) NOT NULL,
    scope_id        VARCHAR(100),
    parameters_json TEXT NOT NULL,
    message         VARCHAR(500) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    version         BIGINT NOT NULL DEFAULT 0,
    valid_from      TIMESTAMPTZ,
    valid_to        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);
