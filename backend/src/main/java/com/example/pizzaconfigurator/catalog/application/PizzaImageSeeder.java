package com.example.pizzaconfigurator.catalog.application;

import com.example.pizzaconfigurator.catalog.domain.Pizza;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.PizzaRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.storage.PizzaImageStorage;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a bundled default photo (backend/src/main/resources/seed-images,
 * see its NOTICE.md for attribution) to any pizza that doesn't have one yet
 * — matched by lowercased pizza code, e.g. MARGHERITA -> seed-images/
 * margherita.jpg. Runs on every startup in every environment (including
 * production) rather than as a one-off migration/ops script, so it also
 * self-heals a fresh deployment or a newly admin-created pizza that happens
 * to reuse a known demo code. A pizza with no matching bundled resource, or
 * one that already has an admin-uploaded image, is left untouched.
 */
@Component
class PizzaImageSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PizzaImageSeeder.class);
    private static final List<String> EXTENSIONS = List.of("jpg", "png", "webp");

    private final PizzaRepository pizzas;
    private final PizzaImageStorage imageStorage;

    PizzaImageSeeder(PizzaRepository pizzas, PizzaImageStorage imageStorage) {
        this.pizzas = pizzas;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Pizza pizza : pizzas.findAll()) {
            if (pizza.getImageUrl() != null) {
                continue;
            }
            seedImage(pizza);
        }
    }

    private void seedImage(Pizza pizza) {
        for (String extension : EXTENSIONS) {
            Resource resource = new ClassPathResource("seed-images/" + pizza.getCode().toLowerCase() + "." + extension);
            if (!resource.exists()) {
                continue;
            }
            try {
                byte[] bytes = resource.getContentAsByteArray();
                String contentType = "jpg".equals(extension) ? "image/jpeg" : "image/" + extension;
                imageStorage.store(pizza.getPizzaId(), contentType, bytes);
                pizza.updateImageUrl("/api/v1/catalog/pizzas/" + pizza.getPizzaId() + "/image");
            } catch (IOException e) {
                log.warn("Could not seed default image for pizza {}", pizza.getCode(), e);
            }
            return;
        }
    }
}
