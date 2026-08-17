-- Phase 5 gap: PriceQuote.priceVersion was returned to the caller but never
-- persisted on the session, so it couldn't be snapshotted onto an OrderItem
-- later (agent.md §5.1 OrderItem.priceVersion). Fixed now, before Order
-- Module needs it.
ALTER TABLE configuration.configuration_session ADD COLUMN price_version VARCHAR(100);
