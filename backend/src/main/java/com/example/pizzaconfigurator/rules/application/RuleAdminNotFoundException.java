package com.example.pizzaconfigurator.rules.application;

import java.util.UUID;

public class RuleAdminNotFoundException extends RuntimeException {

    public RuleAdminNotFoundException(UUID ruleId) {
        super("No rule found for id " + ruleId);
    }
}
