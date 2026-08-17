CREATE TABLE catalog.size (
    size_id       UUID PRIMARY KEY,
    code          VARCHAR(20) NOT NULL UNIQUE,
    display_name  VARCHAR(100) NOT NULL,
    price_modifier NUMERIC(10,2) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE catalog.dough (
    dough_id      UUID PRIMARY KEY,
    code          VARCHAR(20) NOT NULL UNIQUE,
    display_name  VARCHAR(100) NOT NULL,
    price_modifier NUMERIC(10,2) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE catalog.ingredient (
    ingredient_id UUID PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(20) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    default_unit  VARCHAR(20),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE catalog.pizza (
    pizza_id      UUID PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    base_price    NUMERIC(10,2) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    version       BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE catalog.pizza_ingredient (
    pizza_ingredient_id UUID PRIMARY KEY,
    pizza_id      UUID NOT NULL REFERENCES catalog.pizza (pizza_id),
    ingredient_id UUID NOT NULL REFERENCES catalog.ingredient (ingredient_id),
    default_quantity INTEGER NOT NULL DEFAULT 1,
    removable     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    UNIQUE (pizza_id, ingredient_id)
);
