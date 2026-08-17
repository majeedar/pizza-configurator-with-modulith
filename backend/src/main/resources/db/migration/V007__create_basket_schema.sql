CREATE SCHEMA IF NOT EXISTS basket;

CREATE TABLE basket.basket (
    basket_id      UUID PRIMARY KEY,
    customer_id    UUID,
    session_token  VARCHAR(100) NOT NULL UNIQUE,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE basket.basket_item (
    basket_item_id     UUID PRIMARY KEY,
    basket_id          UUID NOT NULL REFERENCES basket.basket (basket_id),
    configuration_id   UUID NOT NULL,
    quantity            INTEGER NOT NULL,
    snapshot_price      NUMERIC(10,2) NOT NULL,
    snapshot_currency   VARCHAR(3) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);
