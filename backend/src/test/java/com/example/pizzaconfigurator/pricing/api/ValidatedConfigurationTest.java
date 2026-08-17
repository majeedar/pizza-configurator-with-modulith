package com.example.pizzaconfigurator.pricing.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * No Spring context, no database — proves the "trusted validation
 * reference" guard (agent.md §7.3) is enforced in-process.
 */
class ValidatedConfigurationTest {

    @Test
    void blankRuleVersionIsRejected() {
        assertThatThrownBy(() -> new ValidatedConfiguration("MARGHERITA", "M", "CLASSIC", List.of(), " "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullRuleVersionIsRejected() {
        assertThatThrownBy(() -> new ValidatedConfiguration("MARGHERITA", "M", "CLASSIC", List.of(), null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
