package com.example.pizzaconfigurator.orders.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stores every snapshot needed to reconstruct the accepted order even if
 * the catalog later changes (agent.md §5.1, §11.1, §12) — historical orders
 * must not silently change when catalog/rules/prices change.
 */
@Entity
@Table(name = "order_item", schema = "orders")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderItemId;

    private UUID orderId;
    private UUID pizzaId;
    private String pizzaNameSnapshot;
    private String sizeCode;
    private String doughCode;
    private int quantity;
    private String modificationsJson;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String ruleVersion;
    private String priceVersion;

    protected OrderItem() {
    }

    public OrderItem(
        UUID orderId,
        UUID pizzaId,
        String pizzaNameSnapshot,
        String sizeCode,
        String doughCode,
        int quantity,
        String modificationsJson,
        BigDecimal unitPrice,
        String ruleVersion,
        String priceVersion
    ) {
        this.orderId = orderId;
        this.pizzaId = pizzaId;
        this.pizzaNameSnapshot = pizzaNameSnapshot;
        this.sizeCode = sizeCode;
        this.doughCode = doughCode;
        this.quantity = quantity;
        this.modificationsJson = modificationsJson;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.ruleVersion = ruleVersion;
        this.priceVersion = priceVersion;
    }

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getPizzaId() {
        return pizzaId;
    }

    public String getPizzaNameSnapshot() {
        return pizzaNameSnapshot;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public String getDoughCode() {
        return doughCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getModificationsJson() {
        return modificationsJson;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public String getPriceVersion() {
        return priceVersion;
    }
}
