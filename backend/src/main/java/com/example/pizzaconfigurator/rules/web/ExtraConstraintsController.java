package com.example.pizzaconfigurator.rules.web;

import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.rules.api.ExtraConstraintsView;
import com.example.pizzaconfigurator.rules.api.RuleConstraintsQuery;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registered under the customer-facing {@code /api/v1/catalog/**} path even
 * though it belongs to the {@code rules} module — same precedent as {@code
 * recommendation.web.KitchenReviewController} under {@code /api/v1/kitchen/reviews}:
 * Spring MVC route registration doesn't require the URL path to match the
 * owning module. Public/permitAll, same as every other {@code /catalog/**}
 * read endpoint.
 */
@RestController
@RequestMapping("/api/v1/catalog/pizzas/{pizzaId}/extra-constraints")
class ExtraConstraintsController {

    private final RuleConstraintsQuery constraintsQuery;
    private final CatalogQuery catalogQuery;

    ExtraConstraintsController(RuleConstraintsQuery constraintsQuery, CatalogQuery catalogQuery) {
        this.constraintsQuery = constraintsQuery;
        this.catalogQuery = catalogQuery;
    }

    @GetMapping
    ExtraConstraintsView get(@PathVariable UUID pizzaId) {
        String pizzaCode = catalogQuery.getPizza(pizzaId).code();
        return constraintsQuery.getExtraConstraints(pizzaCode);
    }
}
