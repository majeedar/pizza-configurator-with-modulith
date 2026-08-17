package com.example.pizzaconfigurator.security.api;

import java.util.UUID;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(UUID employeeId) {
        super("No employee found for id " + employeeId);
    }
}
