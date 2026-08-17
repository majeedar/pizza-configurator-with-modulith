package com.example.pizzaconfigurator.configuration.application;

import com.example.pizzaconfigurator.configuration.api.ConfigurationSessionView;
import com.example.pizzaconfigurator.pricing.api.PriceQuote;

public record PricingOutcome(ConfigurationSessionView session, PriceQuote quote) {
}
