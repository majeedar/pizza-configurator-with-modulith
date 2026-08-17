-- Demo/dev-only app links (agent.md §36) so the "Get the app" UI and QR
-- codes have something to render out of the box — direct-download APK
-- placeholders (agent.md §8.5/§8.6), not real Play Store URLs.
INSERT INTO admin.app_link_setting (app_link_id, platform, audience, url, active, updated_by, updated_at) VALUES
    (gen_random_uuid(), 'ANDROID', 'CUSTOMER', 'https://example.test/downloads/pizza-configurator-customer.apk', true, 'seed', now()),
    (gen_random_uuid(), 'ANDROID', 'KITCHEN',  'https://example.test/downloads/pizza-configurator-kitchen.apk',  true, 'seed', now());
