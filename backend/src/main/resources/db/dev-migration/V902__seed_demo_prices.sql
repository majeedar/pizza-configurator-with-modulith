-- Demo/dev-only price seed data. Same dev-only Flyway location as V900/V901
-- — never applied to a production database.

INSERT INTO pricing.price_definition (price_id, item_type, item_id, amount, currency, active, version, created_at, updated_at) VALUES
    (gen_random_uuid(), 'PIZZA', 'MARGHERITA', 8.50, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'PIZZA', 'HAWAII',     9.50, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'PIZZA', 'NAPOLI',     9.50, 'EUR', true, 0, now(), now()),

    (gen_random_uuid(), 'SIZE', 'S', 0.00, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'SIZE', 'M', 1.50, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'SIZE', 'L', 3.00, 'EUR', true, 0, now(), now()),

    (gen_random_uuid(), 'DOUGH', 'CLASSIC',     0.00, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'DOUGH', 'GLUTEN_FREE', 1.50, 'EUR', true, 0, now(), now()),

    (gen_random_uuid(), 'INGREDIENT', 'TOMATO_SAUCE', 1.00, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'INGREDIENT', 'MOZZARELLA',   1.00, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'INGREDIENT', 'BASIL',        1.00, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'INGREDIENT', 'HAM',          1.20, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'INGREDIENT', 'PINEAPPLE',    1.00, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'INGREDIENT', 'OLIVES',       1.00, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'INGREDIENT', 'CHEESE',       1.30, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'INGREDIENT', 'ONION',        1.00, 'EUR', true, 0, now(), now()),
    (gen_random_uuid(), 'INGREDIENT', 'ANCHOVY',      1.50, 'EUR', true, 0, now(), now());
