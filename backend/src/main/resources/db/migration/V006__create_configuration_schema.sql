CREATE TABLE configuration.configuration_session (
    configuration_id  UUID PRIMARY KEY,
    customer_id       UUID,
    pizza_id          UUID NOT NULL,
    size_code         VARCHAR(20) NOT NULL,
    dough_code        VARCHAR(20) NOT NULL,
    configuration_json TEXT NOT NULL,
    comment           VARCHAR(1000),
    validation_status VARCHAR(20) NOT NULL,
    rule_version      VARCHAR(100),
    price_status      VARCHAR(20) NOT NULL,
    calculated_price  NUMERIC(10,2),
    currency          VARCHAR(3) NOT NULL DEFAULT 'EUR',
    expires_at        TIMESTAMPTZ NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);
