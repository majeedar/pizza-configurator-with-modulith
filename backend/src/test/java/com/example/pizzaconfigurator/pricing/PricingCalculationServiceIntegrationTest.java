package com.example.pizzaconfigurator.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
import com.example.pizzaconfigurator.pricing.api.PriceNotDefinedException;
import com.example.pizzaconfigurator.pricing.api.PriceQuote;
import com.example.pizzaconfigurator.pricing.api.PricedExtra;
import com.example.pizzaconfigurator.pricing.api.PricingService;
import com.example.pizzaconfigurator.pricing.api.ValidatedConfiguration;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves the Phase 4 Definition of Done (agent.md §32 Phase 4): a valid
 * configuration returns a deterministic price, against the demo price
 * data.
 */
@SpringBootTest
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class PricingCalculationServiceIntegrationTest {

    @Autowired
    private PricingService pricingService;

    @Test
    void margheritaMediumClassicNoExtras() {
        ValidatedConfiguration configuration =
            new ValidatedConfiguration("MARGHERITA", "M", "CLASSIC", List.of(), "trusted-rule-version");

        PriceQuote quote = pricingService.calculate(configuration);

        assertThat(quote.currency()).isEqualTo("EUR");
        assertThat(quote.base()).isEqualByComparingTo("8.50");
        assertThat(quote.size()).isEqualByComparingTo("1.50");
        assertThat(quote.dough()).isEqualByComparingTo("0.00");
        assertThat(quote.extras()).isEqualByComparingTo("0.00");
        assertThat(quote.total()).isEqualByComparingTo("10.00");
        assertThat(quote.priceVersion()).isNotBlank();
    }

    @Test
    void margheritaLargeClassicWithExtraCheese() {
        ValidatedConfiguration configuration = new ValidatedConfiguration(
            "MARGHERITA", "L", "CLASSIC", List.of(new PricedExtra("CHEESE", 2)), "trusted-rule-version");

        PriceQuote quote = pricingService.calculate(configuration);

        // 8.50 base + 3.00 (L) + 0.00 (classic) + 2 x 1.30 (cheese) = 14.10
        assertThat(quote.total()).isEqualByComparingTo(new BigDecimal("14.10"));
    }

    @Test
    void zeroQuantityExtraDoesNotRequireAPrice() {
        ValidatedConfiguration configuration = new ValidatedConfiguration(
            "MARGHERITA", "M", "CLASSIC", List.of(new PricedExtra("DOES_NOT_EXIST", 0)), "trusted-rule-version");

        PriceQuote quote = pricingService.calculate(configuration);

        assertThat(quote.total()).isEqualByComparingTo("10.00");
    }

    @Test
    void unknownIngredientWithoutAPriceFails() {
        ValidatedConfiguration configuration = new ValidatedConfiguration(
            "MARGHERITA", "M", "CLASSIC", List.of(new PricedExtra("DOES_NOT_EXIST", 1)), "trusted-rule-version");

        assertThatThrownBy(() -> pricingService.calculate(configuration))
            .isInstanceOf(PriceNotDefinedException.class);
    }

    @Test
    void priceVersionIsStableForTheSameConfiguration() {
        ValidatedConfiguration configuration =
            new ValidatedConfiguration("HAWAII", "S", "GLUTEN_FREE", List.of(), "trusted-rule-version");

        PriceQuote first = pricingService.calculate(configuration);
        PriceQuote second = pricingService.calculate(configuration);

        assertThat(first.priceVersion()).isEqualTo(second.priceVersion());
        assertThat(first.total()).isEqualByComparingTo(second.total());
    }
}
