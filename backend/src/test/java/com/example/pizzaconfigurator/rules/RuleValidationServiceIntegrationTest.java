package com.example.pizzaconfigurator.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import com.example.pizzaconfigurator.rules.api.ExtraSelection;
import com.example.pizzaconfigurator.rules.api.RuleValidation;
import com.example.pizzaconfigurator.rules.api.ValidationResult;
import com.example.pizzaconfigurator.rules.api.ValidationStatus;
import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import com.example.pizzaconfigurator.rules.domain.RuleType;
import com.example.pizzaconfigurator.rules.domain.ScopeType;
import com.example.pizzaconfigurator.rules.infrastructure.persistence.RuleRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves the Phase 3 Definition of Done (agent.md §32 Phase 3): valid and
 * invalid scenarios work end-to-end against the demo rule set, entirely
 * independent of the AI Adapter.
 */
@SpringBootTest
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class RuleValidationServiceIntegrationTest {

    @Autowired
    private RuleValidation ruleValidation;

    @Autowired
    private CatalogQuery catalogQuery;

    @Autowired
    private RuleRepository ruleRepository;

    private UUID pizzaIdFor(String code) {
        return catalogQuery.findActivePizzas().stream()
            .filter(p -> p.code().equals(code))
            .map(PizzaSummary::pizzaId)
            .findFirst()
            .orElseThrow();
    }

    @Test
    void standardMargheritaIsValid() {
        ConfigurationCandidate candidate = new ConfigurationCandidate(
            pizzaIdFor("MARGHERITA"), "M", "CLASSIC", Set.of(), List.of());

        ValidationResult result = ruleValidation.validate(candidate);

        assertThat(result.status()).isEqualTo(ValidationStatus.VALID);
        assertThat(result.violations()).isEmpty();
        assertThat(result.ruleVersion()).isNotBlank();
    }

    @Test
    void extraCheeseWithinMaxIsValid() {
        ConfigurationCandidate candidate = new ConfigurationCandidate(
            pizzaIdFor("MARGHERITA"), "M", "CLASSIC", Set.of(), List.of(new ExtraSelection("CHEESE", 2)));

        ValidationResult result = ruleValidation.validate(candidate);

        assertThat(result.status()).isEqualTo(ValidationStatus.VALID);
    }

    @Test
    void extraCheeseAboveMaxIsInvalidWithSuggestion() {
        ConfigurationCandidate candidate = new ConfigurationCandidate(
            pizzaIdFor("MARGHERITA"), "M", "CLASSIC", Set.of(), List.of(new ExtraSelection("CHEESE", 3)));

        ValidationResult result = ruleValidation.validate(candidate);

        assertThat(result.status()).isEqualTo(ValidationStatus.INVALID);
        assertThat(result.violations()).hasSize(1);
        assertThat(result.violations().get(0).code()).isEqualTo("MAX_QUANTITY_EXCEEDED");
        assertThat(result.suggestions()).hasSize(1);
        assertThat(result.suggestions().get(0).patch()).containsEntry("extras.CHEESE", 2);
    }

    @Test
    void removingNonRemovableBaseIngredientIsInvalid() {
        ConfigurationCandidate candidate = new ConfigurationCandidate(
            pizzaIdFor("MARGHERITA"), "M", "CLASSIC", Set.of("TOMATO_SAUCE"), List.of());

        ValidationResult result = ruleValidation.validate(candidate);

        assertThat(result.status()).isEqualTo(ValidationStatus.INVALID);
        assertThat(result.violations()).extracting("code").containsExactly("REMOVAL_NOT_ALLOWED");
    }

    @Test
    void glutenFreeNapoliIsInvalid() {
        ConfigurationCandidate candidate = new ConfigurationCandidate(
            pizzaIdFor("NAPOLI"), "M", "GLUTEN_FREE", Set.of(), List.of());

        ValidationResult result = ruleValidation.validate(candidate);

        assertThat(result.status()).isEqualTo(ValidationStatus.INVALID);
        assertThat(result.violations()).extracting("code").containsExactly("DOUGH_NOT_AVAILABLE");
    }

    @Test
    void anchovyAndPineappleTogetherOnNapoliIsInvalid() {
        // Napoli's base recipe already includes ANCHOVY (non-removed), so
        // adding PINEAPPLE as an extra triggers the exclusion.
        ConfigurationCandidate candidate = new ConfigurationCandidate(
            pizzaIdFor("NAPOLI"), "M", "CLASSIC", Set.of(), List.of(new ExtraSelection("PINEAPPLE", 1)));

        ValidationResult result = ruleValidation.validate(candidate);

        assertThat(result.status()).isEqualTo(ValidationStatus.INVALID);
        assertThat(result.violations()).extracting("code").containsExactly("INCOMPATIBLE_INGREDIENTS");
    }

    /**
     * Regression test for a real bug found via Admin Portal testing (Phase 15):
     * an {@code OPTION_ALLOWED} rule saved without its required {@code allowed}
     * boolean threw a Jackson {@code MismatchedInputException} on evaluation —
     * and because rules apply on every subsequent validation, not just at save
     * time, a single bad GLOBAL rule 500'd every pizza's checkout, not just the
     * one an admin was testing. {@link
     * com.example.pizzaconfigurator.rules.application.RuleValidationService}
     * now catches a per-rule evaluation failure and skips just that rule
     * (logged) rather than letting it fail the whole request.
     */
    @Test
    void aMisconfiguredRuleIsSkippedRatherThanFailingValidationForEveryPizza() {
        RuleDefinition broken = new RuleDefinition(
            "BROKEN_TEST_RULE", RuleType.OPTION_ALLOWED, ScopeType.GLOBAL, null,
            "{\"ingredientCode\":\"BROKEN_TEST_INGREDIENT\"}", // missing the required "allowed" boolean
            "should never be shown", true, null, null);
        ruleRepository.save(broken);

        try {
            ConfigurationCandidate candidate = new ConfigurationCandidate(
                pizzaIdFor("MARGHERITA"), "M", "CLASSIC", Set.of(), List.of());

            ValidationResult result = ruleValidation.validate(candidate);

            assertThat(result.status()).isEqualTo(ValidationStatus.VALID);
            assertThat(result.violations()).isEmpty();
        } finally {
            ruleRepository.delete(broken);
        }
    }
}
