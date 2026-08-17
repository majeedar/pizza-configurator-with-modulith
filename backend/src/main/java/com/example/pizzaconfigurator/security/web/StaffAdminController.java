package com.example.pizzaconfigurator.security.web;

import com.example.pizzaconfigurator.security.api.StaffManagement;
import com.example.pizzaconfigurator.security.api.StaffView;
import com.example.pizzaconfigurator.security.web.dto.StaffCreateRequest;
import com.example.pizzaconfigurator.security.web.dto.StaffEnabledRequest;
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

/** Agent.md §8.3/§9.3 "Staff Users" — requires {@code ROLE_ADMIN} via the existing {@code /api/v1/admin/**} filter-chain rule. */
@RestController
@RequestMapping("/api/v1/admin/users")
class StaffAdminController {

    private final StaffManagement staffManagement;

    StaffAdminController(StaffManagement staffManagement) {
        this.staffManagement = staffManagement;
    }

    @GetMapping
    List<StaffView> list() {
        return staffManagement.listStaff();
    }

    @PostMapping
    ResponseEntity<StaffView> create(@Valid @RequestBody StaffCreateRequest request) {
        StaffView created = staffManagement.createStaff(
            request.username(), request.displayName(), request.email(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{employeeId}/enabled")
    StaffView setEnabled(@PathVariable UUID employeeId, @RequestBody StaffEnabledRequest request) {
        return staffManagement.setEnabled(employeeId, request.enabled());
    }
}
