package com.example.pizzaconfigurator.notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Agent.md §5.1: one row per channel attempted for a given order event —
 * an order-ready event with both an email address and an
 * {@code Order.fcmDeviceToken} produces two independent rows. Channels
 * never affect each other's outcome (§7.9).
 */
@Entity
@Table(name = "notification_record", schema = "notification")
@EntityListeners(AuditingEntityListener.class)
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    private String recipient;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private String providerMessageId;
    private String failureReason;

    @CreatedDate
    private Instant createdAt;

    private Instant sentAt;

    protected NotificationRecord() {
    }

    private NotificationRecord(
        UUID orderId,
        NotificationChannel channel,
        String recipient,
        NotificationType type,
        NotificationStatus status,
        String providerMessageId,
        String failureReason,
        Instant sentAt
    ) {
        this.orderId = orderId;
        this.channel = channel;
        this.recipient = recipient;
        this.type = type;
        this.status = status;
        this.providerMessageId = providerMessageId;
        this.failureReason = failureReason;
        this.sentAt = sentAt;
    }

    public static NotificationRecord sent(
        UUID orderId, NotificationChannel channel, String recipient, NotificationType type, String providerMessageId, Clock clock
    ) {
        return new NotificationRecord(
            orderId, channel, recipient, type, NotificationStatus.SENT, providerMessageId, null, Instant.now(clock));
    }

    public static NotificationRecord failed(
        UUID orderId, NotificationChannel channel, String recipient, NotificationType type, String failureReason
    ) {
        return new NotificationRecord(
            orderId, channel, recipient, type, NotificationStatus.FAILED, null, failureReason, null);
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
