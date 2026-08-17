package com.example.pizzaconfigurator.basket.domain;

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
 * For the first version, basket is persisted server-side (agent.md §5.1).
 * {@code sessionToken} lets a guest's basket be found across requests
 * without an account.
 */
@Entity
@Table(name = "basket", schema = "basket")
@EntityListeners(AuditingEntityListener.class)
public class Basket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID basketId;

    private UUID customerId;
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    private BasketStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected Basket() {
    }

    public Basket(UUID customerId, String sessionToken) {
        this.customerId = customerId;
        this.sessionToken = sessionToken;
        this.status = BasketStatus.OPEN;
    }

    public UUID getBasketId() {
        return basketId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public BasketStatus getStatus() {
        return status;
    }

    public void markCheckedOut() {
        this.status = BasketStatus.CHECKED_OUT;
    }
}
