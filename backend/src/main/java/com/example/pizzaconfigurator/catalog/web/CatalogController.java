package com.example.pizzaconfigurator.catalog.web;

import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import com.example.pizzaconfigurator.catalog.api.PizzaView;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController {

    private final CatalogQuery catalogQuery;

    CatalogController(CatalogQuery catalogQuery) {
        this.catalogQuery = catalogQuery;
    }

    @GetMapping("/pizzas")
    List<PizzaSummary> findActivePizzas() {
        return catalogQuery.findActivePizzas();
    }

    @GetMapping("/pizzas/{pizzaId}")
    PizzaView getPizza(@PathVariable UUID pizzaId) {
        return catalogQuery.getPizza(pizzaId);
    }

    @GetMapping("/pizzas/{pizzaId}/options")
    ConfigurableOptions getOptions(@PathVariable UUID pizzaId) {
        return catalogQuery.getOptions(pizzaId);
    }
}
