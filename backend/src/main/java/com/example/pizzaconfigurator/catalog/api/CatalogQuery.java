package com.example.pizzaconfigurator.catalog.api;

import java.util.List;
import java.util.UUID;

/**
 * Published read API for the catalog module. No pricing or validity
 * decisions live here — those belong to the Pricing and Rules modules.
 */
public interface CatalogQuery {

    PizzaView getPizza(UUID pizzaId);

    List<PizzaSummary> findActivePizzas();

    ConfigurableOptions getOptions(UUID pizzaId);
}
