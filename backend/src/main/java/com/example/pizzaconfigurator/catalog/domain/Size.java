package com.example.pizzaconfigurator.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "size", schema = "catalog")
@EntityListeners(AuditingEntityListener.class)
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sizeId;

    private String code;
    private String displayName;
    private BigDecimal priceModifier;
    private boolean active;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected Size() {
    }

    public Size(String code, String displayName, BigDecimal priceModifier, boolean active) {
        this.code = code;
        this.displayName = displayName;
        this.priceModifier = priceModifier;
        this.active = active;
    }

    public UUID getSizeId() {
        return sizeId;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getPriceModifier() {
        return priceModifier;
    }

    public boolean isActive() {
        return active;
    }

    public void update(String displayName, BigDecimal priceModifier, boolean active) {
        this.displayName = displayName;
        this.priceModifier = priceModifier;
        this.active = active;
    }
}
