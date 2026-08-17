package com.example.pizzaconfigurator.security.application;

import com.example.pizzaconfigurator.security.api.InvalidStaffCredentialsException;
import com.example.pizzaconfigurator.security.api.StaffAuthResult;
import com.example.pizzaconfigurator.security.api.StaffAuthentication;
import com.example.pizzaconfigurator.security.api.StaffPrincipal;
import com.example.pizzaconfigurator.security.api.StaffView;
import com.example.pizzaconfigurator.security.domain.Employee;
import com.example.pizzaconfigurator.security.infrastructure.persistence.EmployeeRepository;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent.md §14.1: staff (KITCHEN/ADMIN) log in with the same self-issued
 * JWT approach as customers, via {@code POST /api/v1/staff/login}. Token
 * validity is re-checked against the current {@link Employee} row on every
 * {@link #resolveStaff} call (not just the JWT signature/expiry) so a
 * disabled account loses access immediately, without waiting for the token
 * to expire.
 */
@Service
@Transactional
class StaffAuthenticationService implements StaffAuthentication {

    private final EmployeeRepository employees;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    StaffAuthenticationService(EmployeeRepository employees, JwtTokenProvider tokenProvider) {
        this.employees = employees;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffAuthResult login(String username, String rawPassword) {
        Employee employee = employees.findByUsername(username).orElseThrow(InvalidStaffCredentialsException::new);
        if (!employee.isEnabled() || !passwordEncoder.matches(rawPassword, employee.getPasswordHash())) {
            throw new InvalidStaffCredentialsException();
        }
        String token = tokenProvider.issueStaffToken(employee.getEmployeeId(), employee.getUsername(), employee.getRole().name());
        return new StaffAuthResult(toView(employee), token);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StaffPrincipal> resolveStaff(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenProvider.parseStaffClaims(token).flatMap(claims ->
            employees.findById(claims.employeeId())
                .filter(Employee::isEnabled)
                .filter(employee -> employee.getRole().name().equals(claims.role()))
                .map(employee -> new StaffPrincipal(employee.getEmployeeId(), employee.getUsername(), employee.getRole())));
    }

    private StaffView toView(Employee employee) {
        return new StaffView(
            employee.getEmployeeId(), employee.getUsername(), employee.getDisplayName(), employee.getRole(), employee.isEnabled());
    }
}
