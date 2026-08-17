CREATE TABLE security.customer (
    customer_id    UUID PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    email          VARCHAR(320) NOT NULL UNIQUE,
    phone_number   VARCHAR(30),
    password_hash  VARCHAR(200),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);
