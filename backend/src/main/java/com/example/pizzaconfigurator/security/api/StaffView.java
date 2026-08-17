package com.example.pizzaconfigurator.security.api;

import com.example.pizzaconfigurator.security.domain.EmployeeRole;
import java.util.UUID;

public record StaffView(UUID employeeId, String username, String displayName, EmployeeRole role, boolean enabled) {
}
