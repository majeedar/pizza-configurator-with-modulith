package com.example.pizzaconfigurator.catalog.domain;

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

@Entity
@Table(name = "ingredient", schema = "catalog")
@EntityListeners(AuditingEntityListener.class)
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ingredientId;

    private String code;
    private String name;

    @Enumerated(EnumType.STRING)
    private IngredientType type;

    private boolean active;
    private String defaultUnit;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected Ingredient() {
    }

    public Ingredient(String code, String name, IngredientType type, boolean active, String defaultUnit) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.active = active;
        this.defaultUnit = defaultUnit;
    }

    public UUID getIngredientId() {
        return ingredientId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public IngredientType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, IngredientType type, boolean active, String defaultUnit) {
        this.name = name;
        this.type = type;
        this.active = active;
        this.defaultUnit = defaultUnit;
    }
}
