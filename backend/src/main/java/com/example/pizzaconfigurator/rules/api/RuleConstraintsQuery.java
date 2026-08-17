package com.example.pizzaconfigurator.rules.api;

public interface RuleConstraintsQuery {

    ExtraConstraintsView getExtraConstraints(String pizzaCode);
}
