CREATE TABLE pricing.price_definition (
    price_id    UUID PRIMARY KEY,
    item_type   VARCHAR(20) NOT NULL,
    item_id     VARCHAR(100) NOT NULL,
    amount      NUMERIC(10,2) NOT NULL,
    currency    VARCHAR(3) NOT NULL DEFAULT 'EUR',
    valid_from  TIMESTAMPTZ,
    valid_to    TIMESTAMPTZ,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_price_definition_item ON pricing.price_definition (item_type, item_id, active);
