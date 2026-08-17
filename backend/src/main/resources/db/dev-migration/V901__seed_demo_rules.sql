-- Demo/dev-only rule seed data (agent.md §36 "Demo rules"). Same dev-only
-- Flyway location as V900 — never applied to a production database.

INSERT INTO rules.rule_definition
    (rule_id, rule_code, rule_type, scope_type, scope_id, parameters_json, message, active, version, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'CHEESE_MAX_2', 'MAX_QUANTITY', 'GLOBAL', NULL,
     '{"ingredientCode":"CHEESE","max":2}',
     'A maximum of two extra portions of cheese is allowed.', true, 0, now(), now()),

    (gen_random_uuid(), 'TOMATO_SAUCE_NOT_REMOVABLE', 'REMOVAL_ALLOWED', 'GLOBAL', NULL,
     '{"ingredientCode":"TOMATO_SAUCE","removable":false}',
     'Tomato sauce cannot be removed.', true, 0, now(), now()),

    (gen_random_uuid(), 'NAPOLI_NO_GLUTEN_FREE', 'DOUGH_COMPATIBILITY', 'PIZZA', 'NAPOLI',
     '{"doughCode":"GLUTEN_FREE","allowed":false}',
     'Gluten-Free dough is not available for Napoli.', true, 0, now(), now()),

    (gen_random_uuid(), 'ANCHOVY_PINEAPPLE_EXCLUDED', 'EXCLUDES', 'GLOBAL', NULL,
     '{"ingredientA":"ANCHOVY","ingredientB":"PINEAPPLE"}',
     'Anchovy and pineapple cannot be combined.', true, 0, now(), now()),

    (gen_random_uuid(), 'MARGHERITA_NO_PINEAPPLE', 'OPTION_ALLOWED', 'PIZZA', 'MARGHERITA',
     '{"ingredientCode":"PINEAPPLE","allowed":false}',
     'Pineapple is not offered on a Margherita.', true, 0, now(), now());
