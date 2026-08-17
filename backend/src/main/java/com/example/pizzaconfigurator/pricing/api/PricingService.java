package com.example.pizzaconfigurator.pricing.api;

/**
 * Deterministic price calculation for a validated configuration (agent.md
 * §7.3). Never processes an unvalidated configuration — see
 * {@link ValidatedConfiguration}.
 */
public interface PricingService {

    PriceQuote calculate(ValidatedConfiguration configuration);
}
