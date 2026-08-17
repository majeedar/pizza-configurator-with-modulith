package com.example.pizzaconfigurator.security.infrastructure.persistence;

import com.example.pizzaconfigurator.security.domain.Employee;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByUsername(String username);

    boolean existsByUsername(String username);
}
