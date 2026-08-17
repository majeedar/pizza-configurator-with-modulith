package com.example.pizzaconfigurator.configuration.web.dto;

import com.example.pizzaconfigurator.configuration.api.ConfigurationSessionView;
import com.example.pizzaconfigurator.pricing.api.PriceQuote;

public record PriceResponse(ConfigurationSessionView session, PriceQuote quote) {
}
