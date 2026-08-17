package com.example.pizzaconfigurator.orders.web;

import com.example.pizzaconfigurator.orders.api.OrderView;
import com.example.pizzaconfigurator.orders.application.OrderCheckoutResult;
import com.example.pizzaconfigurator.orders.application.OrderService;
import com.example.pizzaconfigurator.orders.web.dto.CreateOrderRequest;
import com.example.pizzaconfigurator.orders.web.dto.OrderCheckoutResponse;
import com.example.pizzaconfigurator.security.api.CustomerAuthentication;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
class OrderController {

    private final OrderService orderService;
    private final CustomerAuthentication customerAuthentication;

    OrderController(OrderService orderService, CustomerAuthentication customerAuthentication) {
        this.orderService = orderService;
        this.customerAuthentication = customerAuthentication;
    }

    @PostMapping
    ResponseEntity<OrderCheckoutResponse> checkout(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateOrderRequest request
    ) {
        UUID customerId = resolveCustomerId(authorization).orElse(null);
        OrderCheckoutResult result = orderService.checkout(
            request.basketId(), customerId, request.customNotes(), request.fcmDeviceToken(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new OrderCheckoutResponse(result.order(), result.rawAccessToken()));
    }

    @GetMapping("/{displayNumber}/status")
    OrderView status(
        @PathVariable String displayNumber,
        @RequestParam(value = "token", required = false) String accessToken,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UUID customerId = resolveCustomerId(authorization).orElse(null);
        return orderService.getGuardedStatus(displayNumber, customerId, accessToken);
    }

    private Optional<UUID> resolveCustomerId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return customerAuthentication.resolveCustomerId(authorizationHeader.substring("Bearer ".length()));
    }
}
