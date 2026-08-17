-- Demo/dev-only seed data (agent.md §6.1, §36). Only applied when the
-- "local" profile's extra Flyway location is active (application-local.yml)
-- — never runs against a production database.

INSERT INTO catalog.size (size_id, code, display_name, price_modifier, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'S', 'Small',  0.00, true, now(), now()),
    (gen_random_uuid(), 'M', 'Medium', 1.50, true, now(), now()),
    (gen_random_uuid(), 'L', 'Large',  3.00, true, now(), now());

INSERT INTO catalog.dough (dough_id, code, display_name, price_modifier, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'CLASSIC',      'Classic',      0.00, true, now(), now()),
    (gen_random_uuid(), 'GLUTEN_FREE',  'Gluten-Free',  1.50, true, now(), now());

INSERT INTO catalog.ingredient (ingredient_id, code, name, type, active, default_unit, created_at, updated_at) VALUES
    (gen_random_uuid(), 'TOMATO_SAUCE', 'Tomato Sauce', 'SAUCE',     true, 'portion (100g)', now(), now()),
    (gen_random_uuid(), 'MOZZARELLA',   'Mozzarella',   'CHEESE',    true, 'portion (100g)', now(), now()),
    (gen_random_uuid(), 'BASIL',        'Basil',        'VEGETABLE', true, 'portion (100g)', now(), now()),
    (gen_random_uuid(), 'HAM',          'Ham',          'MEAT',      true, 'portion (100g)', now(), now()),
    (gen_random_uuid(), 'PINEAPPLE',    'Pineapple',    'OTHER',     true, 'piece',          now(), now()),
    (gen_random_uuid(), 'OLIVES',       'Olives',       'VEGETABLE', true, 'piece',          now(), now()),
    (gen_random_uuid(), 'CHEESE',       'Extra Cheese', 'CHEESE',    true, 'portion (100g)', now(), now()),
    (gen_random_uuid(), 'ONION',        'Onion',        'VEGETABLE', true, 'portion (100g)', now(), now()),
    (gen_random_uuid(), 'ANCHOVY',      'Anchovy',      'OTHER',     true, 'piece',          now(), now());

INSERT INTO catalog.pizza (pizza_id, code, name, description, base_price, active, version, created_at, updated_at) VALUES
    (gen_random_uuid(), 'MARGHERITA', 'Margherita', 'Tomato, mozzarella, basil',           8.50, true, 0, now(), now()),
    (gen_random_uuid(), 'HAWAII',     'Hawaii',     'Tomato, mozzarella, ham, pineapple',  9.50, true, 0, now(), now()),
    (gen_random_uuid(), 'NAPOLI',     'Napoli',     'Tomato, mozzarella, anchovy, olives', 9.50, true, 0, now(), now());

INSERT INTO catalog.pizza_ingredient (pizza_ingredient_id, pizza_id, ingredient_id, default_quantity, removable, created_at, updated_at)
SELECT gen_random_uuid(), p.pizza_id, i.ingredient_id, r.default_quantity, r.removable, now(), now()
FROM (VALUES
    ('MARGHERITA', 'TOMATO_SAUCE', 1, false),
    ('MARGHERITA', 'MOZZARELLA',   1, true),
    ('MARGHERITA', 'BASIL',        1, true),
    ('HAWAII',     'TOMATO_SAUCE', 1, false),
    ('HAWAII',     'MOZZARELLA',   1, true),
    ('HAWAII',     'HAM',          1, true),
    ('HAWAII',     'PINEAPPLE',    1, true),
    ('NAPOLI',     'TOMATO_SAUCE', 1, false),
    ('NAPOLI',     'MOZZARELLA',   1, true),
    ('NAPOLI',     'ANCHOVY',      1, true),
    ('NAPOLI',     'OLIVES',       1, true)
) AS r(pizza_code, ingredient_code, default_quantity, removable)
JOIN catalog.pizza p ON p.code = r.pizza_code
JOIN catalog.ingredient i ON i.code = r.ingredient_code;
