package com.example.pizzaconfigurator.admin.web;

import com.example.pizzaconfigurator.admin.domain.Audience;

/** Agent.md §9's URL convention is lowercase ({@code customer|kitchen}); the enum constants are uppercase. */
final class AudienceParser {

    private AudienceParser() {
    }

    static Audience parse(String value) {
        try {
            return Audience.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidAudienceException(value);
        }
    }
}
