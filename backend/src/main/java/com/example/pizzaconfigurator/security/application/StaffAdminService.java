package com.example.pizzaconfigurator.security.application;

import com.example.pizzaconfigurator.security.api.EmployeeNotFoundException;
import com.example.pizzaconfigurator.security.api.StaffManagement;
import com.example.pizzaconfigurator.security.api.StaffView;
import com.example.pizzaconfigurator.security.api.UsernameAlreadyRegisteredException;
import com.example.pizzaconfigurator.security.domain.Employee;
import com.example.pizzaconfigurator.security.domain.EmployeeRole;
import com.example.pizzaconfigurator.security.infrastructure.persistence.EmployeeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent.md §7.10/§8.3/§9.3: "Staff Users" admin management — separate from
 * {@link StaffAuthenticationService} (a login concern), matching the
 * customer module's {@code CustomerAuthentication}/{@code CustomerQuery}
 * split from Phase 8.
 */
@Service
@Transactional
class StaffAdminService implements StaffManagement {

    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    StaffAdminService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Override
    public StaffView createStaff(String username, String displayName, String email, String rawPassword, EmployeeRole role) {
        if (employees.existsByUsername(username)) {
            throw new UsernameAlreadyRegisteredException(username);
        }
        Employee employee = employees.save(
            new Employee(username, displayName, email, passwordEncoder.encode(rawPassword), role));
        return toView(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffView> listStaff() {
        return employees.findAll().stream().map(this::toView).toList();
    }

    @Override
    public StaffView setEnabled(UUID employeeId, boolean enabled) {
        Employee employee = employees.findById(employeeId).orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        employee.setEnabled(enabled);
        return toView(employee);
    }

    private StaffView toView(Employee employee) {
        return new StaffView(
            employee.getEmployeeId(), employee.getUsername(), employee.getDisplayName(), employee.getRole(), employee.isEnabled());
    }
}
