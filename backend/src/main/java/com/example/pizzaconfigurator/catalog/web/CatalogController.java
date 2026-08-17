package com.example.pizzaconfigurator.catalog.web;

import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import com.example.pizzaconfigurator.catalog.api.PizzaView;
import com.example.pizzaconfigurator.catalog.infrastructure.storage.PizzaImageStorage;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController {

    private final CatalogQuery catalogQuery;
    private final PizzaImageStorage imageStorage;

    CatalogController(CatalogQuery catalogQuery, PizzaImageStorage imageStorage) {
        this.catalogQuery = catalogQuery;
        this.imageStorage = imageStorage;
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

    @GetMapping("/pizzas/{pizzaId}/image")
    ResponseEntity<byte[]> getImage(@PathVariable UUID pizzaId) {
        return imageStorage.load(pizzaId)
            .map(image -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .body(image.bytes()))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
