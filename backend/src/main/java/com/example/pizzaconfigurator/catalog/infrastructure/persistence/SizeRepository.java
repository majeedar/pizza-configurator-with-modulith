package com.example.pizzaconfigurator.catalog.infrastructure.persistence;

import com.example.pizzaconfigurator.catalog.domain.Size;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, UUID> {

    List<Size> findByActiveTrue();

    Optional<Size> findByCode(String code);
}
