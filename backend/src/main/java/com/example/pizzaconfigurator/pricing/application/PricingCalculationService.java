package com.example.pizzaconfigurator.pricing.application;

import com.example.pizzaconfigurator.pricing.api.PriceNotDefinedException;
import com.example.pizzaconfigurator.pricing.api.PriceQuote;
import com.example.pizzaconfigurator.pricing.api.PricedExtra;
import com.example.pizzaconfigurator.pricing.api.PricingService;
import com.example.pizzaconfigurator.pricing.api.ValidatedConfiguration;
import com.example.pizzaconfigurator.pricing.domain.ItemType;
import com.example.pizzaconfigurator.pricing.domain.PriceDefinition;
import com.example.pizzaconfigurator.pricing.infrastructure.persistence.PriceDefinitionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * total = pizzaBasePrice + sizeModifier + doughModifier +
 * sum(extraIngredientPrice × extraQuantity) — agent.md §12. Deterministic:
 * given the same {@link ValidatedConfiguration} and the same active price
 * rows, always returns the same {@link PriceQuote}.
 */
@Service
@Transactional(readOnly = true)
class PricingCalculationService implements PricingService {

    private final PriceDefinitionRepository prices;
    private final Clock clock;

    PricingCalculationService(PriceDefinitionRepository prices, Clock clock) {
        this.prices = prices;
        this.clock = clock;
    }

    @Override
    public PriceQuote calculate(ValidatedConfiguration configuration) {
        Instant now = Instant.now(clock);
        List<PriceDefinition> used = new ArrayList<>();

        PriceDefinition base = requirePrice(ItemType.PIZZA, configuration.pizzaCode(), now, used);
        PriceDefinition size = requirePrice(ItemType.SIZE, configuration.sizeCode(), now, used);
        PriceDefinition dough = requirePrice(ItemType.DOUGH, configuration.doughCode(), now, used);

        BigDecimal extrasTotal = BigDecimal.ZERO;
        for (PricedExtra extra : configuration.extras()) {
            if (extra.quantity() == 0) {
                continue;
            }
            PriceDefinition extraPrice = requirePrice(ItemType.INGREDIENT, extra.ingredientCode(), now, used);
            extrasTotal = extrasTotal.add(extraPrice.getAmount().multiply(BigDecimal.valueOf(extra.quantity())));
        }

        BigDecimal total = base.getAmount().add(size.getAmount()).add(dough.getAmount()).add(extrasTotal);

        return new PriceQuote(
            base.getCurrency(),
            scale(base.getAmount()),
            scale(size.getAmount()),
            scale(dough.getAmount()),
            scale(extrasTotal),
            scale(total),
            computePriceVersion(used)
        );
    }

    private PriceDefinition requirePrice(ItemType type, String itemId, Instant now, List<PriceDefinition> used) {
        PriceDefinition price = prices.findActive(type, itemId)
            .filter(p -> p.isCurrentlyValid(now))
            .orElseThrow(() -> new PriceNotDefinedException(type, itemId));
        used.add(price);
        return price;
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Changes whenever any price row used in this calculation is created,
     * updated, or deactivated — mirrors the rule-version signature in
     * {@code RuleValidationService} (agent.md §11.1, §12).
     */
    private String computePriceVersion(List<PriceDefinition> used) {
        String signature = used.stream()
            .sorted(Comparator.comparing((PriceDefinition p) -> p.getItemType().name()).thenComparing(PriceDefinition::getItemId))
            .map(p -> p.getItemType() + ":" + p.getItemId() + ":" + p.getVersion())
            .reduce((a, b) -> a + ";" + b)
            .orElse("");
        return Integer.toHexString(signature.hashCode());
    }
}
