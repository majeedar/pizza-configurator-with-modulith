-- Demo/dev-only staff accounts (agent.md §6.1, §36) so Phase 7's KDS can be
-- exercised locally without an Admin Portal (that's Phase 11) to create
-- them. Passwords are the username-matching "<role>123" — local/dev only,
-- never acceptable in a real deployment.
INSERT INTO security.employee (employee_id, username, display_name, email, password_hash, role, enabled, created_at, updated_at) VALUES
    (gen_random_uuid(), 'kitchen', 'Demo Kitchen Staff', 'kitchen@example.test', '$2a$10$KXoA1ODez9tOOBUl9k5joObZTwGBaXsruACeQQuzwzUhsqeGHbi2y', 'KITCHEN', true, now(), now()),
    (gen_random_uuid(), 'admin',   'Demo Admin',         'admin@example.test',   '$2a$10$wFVHTGj7hCCCZQFAVvyEHul.dLo4nJ0y2gDM/L3E1Gb.Ac/Vd5n7S', 'ADMIN',   true, now(), now());
