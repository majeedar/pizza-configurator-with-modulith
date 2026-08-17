package com.example.pizzaconfigurator.security.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Staff account (agent.md §5.1 {@code Employee}, §14.1) — logs in via
 * {@code POST /api/v1/staff/login}, shared by the staff web app and the
 * native kitchen Android app. Distinct from {@link Customer}: no guest
 * concept, {@code passwordHash} is always set.
 */
@Entity
@Table(name = "employee", schema = "security")
@EntityListeners(AuditingEntityListener.class)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID employeeId;

    private String username;
    private String displayName;
    private String email;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private EmployeeRole role;

    private boolean enabled;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected Employee() {
    }

    public Employee(String username, String displayName, String email, String passwordHash, EmployeeRole role) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = true;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
