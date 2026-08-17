package com.example.pizzaconfigurator.pricing.domain;

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
 * The authoritative, versioned price for one catalog item (identified by
 * its stable code, in {@code itemId}) — separate from any reference price
 * shown in the catalog listing (agent.md §5.1, §7.3). Historical orders
 * must keep their accepted snapshot even if this changes later (§12).
 */
@Entity
@Table(name = "price_definition", schema = "pricing")
@EntityListeners(AuditingEntityListener.class)
public class PriceDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID priceId;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    private String itemId;

    private BigDecimal amount;

    private String currency;

    private Instant validFrom;
    private Instant validTo;

    private boolean active;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected PriceDefinition() {
    }

    public PriceDefinition(
        ItemType itemType,
        String itemId,
        BigDecimal amount,
        String currency,
        boolean active,
        Instant validFrom,
        Instant validTo
    ) {
        this.itemType = itemType;
        this.itemId = itemId;
        this.amount = amount;
        this.currency = currency;
        this.active = active;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public UUID getPriceId() {
        return priceId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getItemId() {
        return itemId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public boolean isActive() {
        return active;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isCurrentlyValid(Instant now) {
        return (validFrom == null || !now.isBefore(validFrom)) && (validTo == null || !now.isAfter(validTo));
    }

    public void update(BigDecimal amount, String currency, boolean active, Instant validFrom, Instant validTo) {
        this.amount = amount;
        this.currency = currency;
        this.active = active;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
}
