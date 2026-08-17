package com.example.pizzaconfigurator.notification.infrastructure.persistence;

import com.example.pizzaconfigurator.notification.domain.NotificationRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, UUID> {

    List<NotificationRecord> findByOrderId(UUID orderId);
}
