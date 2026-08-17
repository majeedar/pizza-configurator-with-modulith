package com.example.pizzaconfigurator.admin.application;

import com.example.pizzaconfigurator.admin.api.AuditEntry;
import com.example.pizzaconfigurator.admin.api.AuditLog;
import com.example.pizzaconfigurator.admin.domain.AuditEvent;
import com.example.pizzaconfigurator.admin.infrastructure.persistence.AuditEventRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agent.md §14.4/§30. */
@Service
@Transactional
public class AuditLogService implements AuditLog {

    private final AuditEventRepository events;
    private final Clock clock;

    AuditLogService(AuditEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(AuditEntry entry) {
        events.save(new AuditEvent(
            entry.actorId(), entry.actorRole(), entry.action(), entry.entityType(), entry.entityId(),
            entry.beforeJson(), entry.afterJson(), UUID.randomUUID().toString(), clock));
    }

    @Transactional(readOnly = true)
    public List<AuditEventView> findAll() {
        return events.findAllByOrderByTimestampDesc().stream().map(this::toView).toList();
    }

    private AuditEventView toView(AuditEvent event) {
        return new AuditEventView(
            event.getEventId(), event.getTimestamp(), event.getActorId(), event.getActorRole(), event.getAction(),
            event.getEntityType(), event.getEntityId(), event.getBeforeJson(), event.getAfterJson(), event.getCorrelationId());
    }
}
