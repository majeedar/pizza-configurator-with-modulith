package com.example.pizzaconfigurator.pricing.web;

import com.example.pizzaconfigurator.pricing.application.PriceAdminService;
import com.example.pizzaconfigurator.pricing.domain.PriceDefinition;
import com.example.pizzaconfigurator.pricing.web.dto.PriceAdminRequest;
import com.example.pizzaconfigurator.pricing.web.dto.PriceAdminResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin CRUD for price configuration (agent.md §7.10, §9.3) — requires
 * {@code ROLE_ADMIN} via the {@code /api/v1/admin/**} Spring Security
 * filter-chain rule (agent.md §14.2, Phase 7).
 */
@RestController
@RequestMapping("/api/v1/admin/prices")
class PriceAdminController {

    private final PriceAdminService adminService;

    PriceAdminController(PriceAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    List<PriceAdminResponse> findAll() {
        return adminService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{priceId}")
    PriceAdminResponse getPrice(@PathVariable UUID priceId) {
        return toResponse(adminService.getPrice(priceId));
    }

    @PostMapping
    ResponseEntity<PriceAdminResponse> createPrice(@Valid @RequestBody PriceAdminRequest request) {
        PriceDefinition price = adminService.createPrice(
            request.itemType(), request.itemId(), request.amount(), request.currency(),
            request.active(), request.validFrom(), request.validTo());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(price));
    }

    @PutMapping("/{priceId}")
    PriceAdminResponse updatePrice(@PathVariable UUID priceId, @Valid @RequestBody PriceAdminRequest request) {
        PriceDefinition price = adminService.updatePrice(
            priceId, request.amount(), request.currency(), request.active(), request.validFrom(), request.validTo());
        return toResponse(price);
    }

    private PriceAdminResponse toResponse(PriceDefinition price) {
        return new PriceAdminResponse(
            price.getPriceId(), price.getItemType(), price.getItemId(), price.getAmount(),
            price.getCurrency(), price.isActive(), price.getVersion(), price.getValidFrom(), price.getValidTo());
    }
}
