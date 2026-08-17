-- "order" is a reserved SQL keyword, so the table is named customer_order.
CREATE SEQUENCE orders.display_number_seq START WITH 1;

CREATE TABLE orders.customer_order (
    order_id           UUID PRIMARY KEY,
    display_number     VARCHAR(20) NOT NULL UNIQUE,
    status              VARCHAR(20) NOT NULL,
    total_price         NUMERIC(10,2) NOT NULL,
    customer_id         UUID,
    custom_notes        VARCHAR(1000),
    pickup_token        VARCHAR(20) NOT NULL,
    access_token_hash   VARCHAR(100),
    fcm_device_token     VARCHAR(500),
    currency             VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    version               BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE orders.order_item (
    order_item_id       UUID PRIMARY KEY,
    order_id             UUID NOT NULL REFERENCES orders.customer_order (order_id),
    pizza_id              UUID NOT NULL,
    pizza_name_snapshot   VARCHAR(100) NOT NULL,
    size_code             VARCHAR(20) NOT NULL,
    dough_code            VARCHAR(20) NOT NULL,
    quantity               INTEGER NOT NULL,
    modifications_json    TEXT,
    unit_price             NUMERIC(10,2) NOT NULL,
    subtotal                NUMERIC(10,2) NOT NULL,
    rule_version            VARCHAR(100),
    price_version            VARCHAR(100)
);

CREATE TABLE orders.idempotency_key (
    idempotency_key      VARCHAR(200) PRIMARY KEY,
    request_hash          VARCHAR(100) NOT NULL,
    order_id               UUID NOT NULL REFERENCES orders.customer_order (order_id),
    created_at              TIMESTAMPTZ NOT NULL
);
