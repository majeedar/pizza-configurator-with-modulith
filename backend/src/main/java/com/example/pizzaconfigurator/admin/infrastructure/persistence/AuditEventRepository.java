package com.example.pizzaconfigurator.admin.infrastructure.persistence;

import com.example.pizzaconfigurator.admin.domain.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findAllByOrderByTimestampDesc();
}
