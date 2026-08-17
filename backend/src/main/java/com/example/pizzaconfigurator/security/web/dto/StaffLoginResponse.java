package com.example.pizzaconfigurator.security.web.dto;

import com.example.pizzaconfigurator.security.domain.EmployeeRole;
import java.util.UUID;

public record StaffLoginResponse(UUID employeeId, String username, String displayName, EmployeeRole role, String token) {
}
