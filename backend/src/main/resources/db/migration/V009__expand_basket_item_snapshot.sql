-- Basket items must be immutable, self-contained snapshots (agent.md §7.6),
-- not just price — a customer can still edit the same ConfigurationSession
-- after adding it to the basket, so Order Module cannot safely re-read the
-- session at checkout time. Capture everything OrderItem will need now.
ALTER TABLE basket.basket_item ADD COLUMN pizza_id UUID;
ALTER TABLE basket.basket_item ADD COLUMN pizza_code VARCHAR(50);
ALTER TABLE basket.basket_item ADD COLUMN pizza_name_snapshot VARCHAR(100);
ALTER TABLE basket.basket_item ADD COLUMN size_code VARCHAR(20);
ALTER TABLE basket.basket_item ADD COLUMN dough_code VARCHAR(20);
ALTER TABLE basket.basket_item ADD COLUMN modifications_json TEXT;
ALTER TABLE basket.basket_item ADD COLUMN rule_version VARCHAR(100);
ALTER TABLE basket.basket_item ADD COLUMN price_version VARCHAR(100);
