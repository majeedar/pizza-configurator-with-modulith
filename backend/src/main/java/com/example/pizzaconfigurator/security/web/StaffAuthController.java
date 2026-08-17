package com.example.pizzaconfigurator.security.web;

import com.example.pizzaconfigurator.security.api.StaffAuthResult;
import com.example.pizzaconfigurator.security.api.StaffAuthentication;
import com.example.pizzaconfigurator.security.web.dto.StaffLoginRequest;
import com.example.pizzaconfigurator.security.web.dto.StaffLoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shared login for both {@code ROLE_KITCHEN} and {@code ROLE_ADMIN}
 * (agent.md §8.4, §9.2) — unauthenticated endpoint, every other
 * {@code /api/v1/staff/**}/{@code /api/v1/kitchen/**}/{@code /api/v1/admin/**}
 * endpoint requires the resulting JWT.
 */
@RestController
@RequestMapping("/api/v1/staff")
class StaffAuthController {

    private final StaffAuthentication staffAuthentication;

    StaffAuthController(StaffAuthentication staffAuthentication) {
        this.staffAuthentication = staffAuthentication;
    }

    @PostMapping("/login")
    StaffLoginResponse login(@Valid @RequestBody StaffLoginRequest request) {
        StaffAuthResult result = staffAuthentication.login(request.username(), request.password());
        return new StaffLoginResponse(
            result.staff().employeeId(), result.staff().username(), result.staff().displayName(),
            result.staff().role(), result.token());
    }
}
