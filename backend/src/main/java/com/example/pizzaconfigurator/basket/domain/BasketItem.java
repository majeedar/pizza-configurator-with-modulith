package com.example.pizzaconfigurator.basket.domain;

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

/**
 * {@code configurationId} references a Configuration-module session by id
 * only — no JPA relationship across the module boundary. Every field here
 * is an immutable snapshot taken at add-time (agent.md §7.6) — not just the
 * price. The customer can still edit the same ConfigurationSession after
 * adding it to the basket, so Order Module must never re-read the session
 * at checkout; it reads this snapshot instead.
 */
@Entity
@Table(name = "basket_item", schema = "basket")
@EntityListeners(AuditingEntityListener.class)
public class BasketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID basketItemId;

    private UUID basketId;
    private UUID configurationId;
    private int quantity;

    private UUID pizzaId;
    private String pizzaCode;
    private String pizzaNameSnapshot;
    private String sizeCode;
    private String doughCode;
    private String modificationsJson;
    private String ruleVersion;
    private String priceVersion;

    private BigDecimal snapshotPrice;
    private String snapshotCurrency;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected BasketItem() {
    }

    public BasketItem(
        UUID basketId,
        UUID configurationId,
        int quantity,
        UUID pizzaId,
        String pizzaCode,
        String pizzaNameSnapshot,
        String sizeCode,
        String doughCode,
        String modificationsJson,
        String ruleVersion,
        String priceVersion,
        BigDecimal snapshotPrice,
        String snapshotCurrency
    ) {
        this.basketId = basketId;
        this.configurationId = configurationId;
        this.quantity = quantity;
        this.pizzaId = pizzaId;
        this.pizzaCode = pizzaCode;
        this.pizzaNameSnapshot = pizzaNameSnapshot;
        this.sizeCode = sizeCode;
        this.doughCode = doughCode;
        this.modificationsJson = modificationsJson;
        this.ruleVersion = ruleVersion;
        this.priceVersion = priceVersion;
        this.snapshotPrice = snapshotPrice;
        this.snapshotCurrency = snapshotCurrency;
    }

    public UUID getBasketItemId() {
        return basketItemId;
    }

    public UUID getBasketId() {
        return basketId;
    }

    public UUID getConfigurationId() {
        return configurationId;
    }

    public int getQuantity() {
        return quantity;
    }

    public UUID getPizzaId() {
        return pizzaId;
    }

    public String getPizzaCode() {
        return pizzaCode;
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

    public String getModificationsJson() {
        return modificationsJson;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public String getPriceVersion() {
        return priceVersion;
    }

    public BigDecimal getSnapshotPrice() {
        return snapshotPrice;
    }

    public String getSnapshotCurrency() {
        return snapshotCurrency;
    }

    public BigDecimal lineTotal() {
        return snapshotPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
