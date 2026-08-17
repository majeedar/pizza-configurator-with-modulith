package com.example.pizzaconfigurator.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

@Entity
@Table(name = "pizza", schema = "catalog")
@EntityListeners(AuditingEntityListener.class)
public class Pizza {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pizzaId;

    private String code;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private boolean active;
    private String imageUrl;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected Pizza() {
    }

    public Pizza(String code, String name, String description, BigDecimal basePrice, boolean active) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.active = active;
    }

    public UUID getPizzaId() {
        return pizzaId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public boolean isActive() {
        return active;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getVersion() {
        return version;
    }

    public void update(String name, String description, BigDecimal basePrice, boolean active) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.active = active;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
