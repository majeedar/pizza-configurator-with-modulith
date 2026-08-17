package com.example.pizzaconfigurator.basket.web;

import com.example.pizzaconfigurator.basket.application.BasketService;
import com.example.pizzaconfigurator.basket.application.BasketView;
import com.example.pizzaconfigurator.basket.web.dto.AddBasketItemRequest;
import com.example.pizzaconfigurator.security.api.CustomerAuthentication;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/baskets")
class BasketController {

    private final BasketService basketService;
    private final CustomerAuthentication customerAuthentication;

    BasketController(BasketService basketService, CustomerAuthentication customerAuthentication) {
        this.basketService = basketService;
        this.customerAuthentication = customerAuthentication;
    }

    @PostMapping
    ResponseEntity<BasketView> create(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UUID customerId = resolveCustomerId(authorization).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(basketService.createBasket(customerId));
    }

    @GetMapping("/{basketId}")
    BasketView get(@PathVariable UUID basketId) {
        return basketService.getBasket(basketId);
    }

    @PostMapping("/{basketId}/items")
    BasketView addItem(@PathVariable UUID basketId, @Valid @RequestBody AddBasketItemRequest request) {
        return basketService.addItem(basketId, request.configurationId(), request.quantity());
    }

    @DeleteMapping("/{basketId}/items/{itemId}")
    BasketView removeItem(@PathVariable UUID basketId, @PathVariable UUID itemId) {
        return basketService.removeItem(basketId, itemId);
    }

    private Optional<UUID> resolveCustomerId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return customerAuthentication.resolveCustomerId(authorizationHeader.substring("Bearer ".length()));
    }
}
