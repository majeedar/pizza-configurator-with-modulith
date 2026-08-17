package com.example.pizzaconfigurator.orders.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Created only after the customer confirms the final price (agent.md
 * §7.7). {@code accessTokenHash} is populated only for guest orders (never
 * the raw token — see {@code OrderAccessTokens}); {@code fcmDeviceToken}
 * stays null until native apps exist (Phase 12).
 */
@Entity
@Table(name = "customer_order", schema = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    private String displayNumber;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalPrice;
    private UUID customerId;
    private String customNotes;
    private String pickupToken;
    private String accessTokenHash;
    private String fcmDeviceToken;
    private String currency;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected Order() {
    }

    public Order(
        String displayNumber,
        BigDecimal totalPrice,
        UUID customerId,
        String customNotes,
        String pickupToken,
        String accessTokenHash,
        String fcmDeviceToken,
        String currency
    ) {
        this.displayNumber = displayNumber;
        this.status = OrderStatus.CONFIRMED;
        this.totalPrice = totalPrice;
        this.customerId = customerId;
        this.customNotes = customNotes;
        this.pickupToken = pickupToken;
        this.accessTokenHash = accessTokenHash;
        this.fcmDeviceToken = fcmDeviceToken;
        this.currency = currency;
    }

    public void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateOrderTransitionException(status, next);
        }
        this.status = next;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getDisplayNumber() {
        return displayNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getCustomNotes() {
        return customNotes;
    }

    public String getPickupToken() {
        return pickupToken;
    }

    public String getAccessTokenHash() {
        return accessTokenHash;
    }

    public String getCurrency() {
        return currency;
    }

    public String getFcmDeviceToken() {
        return fcmDeviceToken;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
