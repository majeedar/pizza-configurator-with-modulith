package com.example.pizzaconfigurator.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * The base recipe: which ingredients a pizza starts with, in what default
 * quantity, and whether the customer is allowed to remove them.
 */
@Entity
@Table(name = "pizza_ingredient", schema = "catalog")
@EntityListeners(AuditingEntityListener.class)
public class PizzaIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pizzaIngredientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pizza_id", nullable = false)
    private Pizza pizza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    private int defaultQuantity;
    private boolean removable;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected PizzaIngredient() {
    }

    public PizzaIngredient(Pizza pizza, Ingredient ingredient, int defaultQuantity, boolean removable) {
        this.pizza = pizza;
        this.ingredient = ingredient;
        this.defaultQuantity = defaultQuantity;
        this.removable = removable;
    }

    public UUID getPizzaIngredientId() {
        return pizzaIngredientId;
    }

    public Pizza getPizza() {
        return pizza;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getDefaultQuantity() {
        return defaultQuantity;
    }

    public boolean isRemovable() {
        return removable;
    }

    public void update(int defaultQuantity, boolean removable) {
        this.defaultQuantity = defaultQuantity;
        this.removable = removable;
    }
}
