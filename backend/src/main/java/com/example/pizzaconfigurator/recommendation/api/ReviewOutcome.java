package com.example.pizzaconfigurator.recommendation.api;

import com.example.pizzaconfigurator.configuration.api.ConfigurationSessionView;

public record ReviewOutcome(ReviewRequestView reviewRequest, ConfigurationSessionView session) {
}
