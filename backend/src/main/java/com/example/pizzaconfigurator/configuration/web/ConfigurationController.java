package com.example.pizzaconfigurator.configuration.web;

import com.example.pizzaconfigurator.configuration.api.ConfigurationSessionView;
import com.example.pizzaconfigurator.configuration.application.ConfigurationService;
import com.example.pizzaconfigurator.configuration.application.PricingOutcome;
import com.example.pizzaconfigurator.configuration.application.ValidationOutcome;
import com.example.pizzaconfigurator.configuration.domain.SelectionSnapshot;
import com.example.pizzaconfigurator.configuration.web.dto.ConfigurationRequest;
import com.example.pizzaconfigurator.configuration.web.dto.ExtraRequest;
import com.example.pizzaconfigurator.configuration.web.dto.PriceResponse;
import com.example.pizzaconfigurator.configuration.web.dto.ValidationResponse;
import com.example.pizzaconfigurator.security.api.CustomerAuthentication;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing configuration endpoints (agent.md §9.1). Works for both
 * guests (no {@code Authorization} header) and logged-in customers (a
 * bearer token from {@code /api/v1/customers/login}) — see agent.md §14.1.
 */
@RestController
@RequestMapping("/api/v1/configurations")
class ConfigurationController {

    private final ConfigurationService configurationService;
    private final CustomerAuthentication customerAuthentication;

    ConfigurationController(ConfigurationService configurationService, CustomerAuthentication customerAuthentication) {
        this.configurationService = configurationService;
        this.customerAuthentication = customerAuthentication;
    }

    @PostMapping
    ResponseEntity<ConfigurationSessionView> create(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody ConfigurationRequest request
    ) {
        UUID customerId = resolveCustomerId(authorization).orElse(null);
        ConfigurationSessionView view = configurationService.createSession(
            customerId, request.pizzaId(), request.sizeCode(), request.doughCode(),
            request.removedIngredientsOrEmpty(), toExtras(request.extrasOrEmpty()), request.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PutMapping("/{configurationId}")
    ConfigurationSessionView update(@PathVariable UUID configurationId, @Valid @RequestBody ConfigurationRequest request) {
        return configurationService.updateSession(
            configurationId, request.pizzaId(), request.sizeCode(), request.doughCode(),
            request.removedIngredientsOrEmpty(), toExtras(request.extrasOrEmpty()), request.comment());
    }

    @PostMapping("/{configurationId}/validate")
    ValidationResponse validate(@PathVariable UUID configurationId) {
        ValidationOutcome outcome = configurationService.validateSession(configurationId);
        return new ValidationResponse(outcome.session(), outcome.violations(), outcome.suggestions());
    }

    @PostMapping("/{configurationId}/price")
    PriceResponse price(@PathVariable UUID configurationId) {
        PricingOutcome outcome = configurationService.priceSession(configurationId);
        return new PriceResponse(outcome.session(), outcome.quote());
    }

    @GetMapping("/{configurationId}")
    ConfigurationSessionView get(@PathVariable UUID configurationId) {
        return configurationService.getSession(configurationId);
    }

    private Optional<UUID> resolveCustomerId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return customerAuthentication.resolveCustomerId(authorizationHeader.substring("Bearer ".length()));
    }

    private List<SelectionSnapshot.Extra> toExtras(List<ExtraRequest> extras) {
        return extras.stream().map(e -> new SelectionSnapshot.Extra(e.ingredientCode(), e.quantity())).toList();
    }
}
