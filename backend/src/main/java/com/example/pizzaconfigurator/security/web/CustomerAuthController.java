package com.example.pizzaconfigurator.security.web;

import com.example.pizzaconfigurator.security.api.AuthResult;
import com.example.pizzaconfigurator.security.api.CustomerAuthentication;
import com.example.pizzaconfigurator.security.web.dto.AuthResponse;
import com.example.pizzaconfigurator.security.web.dto.CustomerLoginRequest;
import com.example.pizzaconfigurator.security.web.dto.CustomerRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guest checkout stays fully supported elsewhere — this is only for
 * customers who choose to register/log in (agent.md §9.1, §14.1).
 */
@RestController
@RequestMapping("/api/v1/customers")
class CustomerAuthController {

    private final CustomerAuthentication customerAuthentication;

    CustomerAuthController(CustomerAuthentication customerAuthentication) {
        this.customerAuthentication = customerAuthentication;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        AuthResult result = customerAuthentication.register(
            request.name(), request.email(), request.phoneNumber(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody CustomerLoginRequest request) {
        return toResponse(customerAuthentication.login(request.email(), request.password()));
    }

    private AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(
            result.customer().customerId(), result.customer().name(), result.customer().email(), result.token());
    }
}
