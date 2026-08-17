package com.example.pizzaconfigurator.configuration.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * A customer's configuration before an Order exists (agent.md §5.1, §7.5).
 * {@code sizeCode}/{@code doughCode} — not catalog UUIDs — to match how
 * Rules and Pricing already identify size/dough throughout the codebase.
 */
@Entity
@Table(name = "configuration_session", schema = "configuration")
@EntityListeners(AuditingEntityListener.class)
public class ConfigurationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID configurationId;

    private UUID customerId;
    private UUID pizzaId;
    private String sizeCode;
    private String doughCode;
    private String configurationJson;
    private String comment;

    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus;

    private String ruleVersion;

    @Enumerated(EnumType.STRING)
    private PriceStatus priceStatus;

    private BigDecimal calculatedPrice;
    private String priceVersion;
    private String currency;
    private Instant expiresAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected ConfigurationSession() {
    }

    public ConfigurationSession(
        UUID customerId, UUID pizzaId, String sizeCode, String doughCode,
        String configurationJson, String comment, Instant expiresAt
    ) {
        this.customerId = customerId;
        this.pizzaId = pizzaId;
        this.sizeCode = sizeCode;
        this.doughCode = doughCode;
        this.configurationJson = configurationJson;
        this.comment = comment;
        this.validationStatus = ValidationStatus.DRAFT;
        this.priceStatus = PriceStatus.UNPRICED;
        this.currency = "EUR";
        this.expiresAt = expiresAt;
    }

    public void updateSelections(UUID pizzaId, String sizeCode, String doughCode, String configurationJson, String comment) {
        this.pizzaId = pizzaId;
        this.sizeCode = sizeCode;
        this.doughCode = doughCode;
        this.configurationJson = configurationJson;
        this.comment = comment;
        // Any change invalidates a previous verdict — must revalidate/reprice.
        this.validationStatus = ValidationStatus.DRAFT;
        this.priceStatus = PriceStatus.UNPRICED;
        this.ruleVersion = null;
        this.calculatedPrice = null;
        this.priceVersion = null;
    }

    public void markValidated(ValidationStatus status, String ruleVersion) {
        this.validationStatus = status;
        this.ruleVersion = ruleVersion;
        this.priceStatus = PriceStatus.UNPRICED;
        this.calculatedPrice = null;
        this.priceVersion = null;
    }

    public void markPriced(BigDecimal total, String currency, String priceVersion) {
        this.calculatedPrice = total;
        this.currency = currency;
        this.priceVersion = priceVersion;
        this.priceStatus = PriceStatus.READY_FOR_CHECKOUT;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public UUID getConfigurationId() {
        return configurationId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getPizzaId() {
        return pizzaId;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public String getDoughCode() {
        return doughCode;
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public String getComment() {
        return comment;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public PriceStatus getPriceStatus() {
        return priceStatus;
    }

    public BigDecimal getCalculatedPrice() {
        return calculatedPrice;
    }

    public String getPriceVersion() {
        return priceVersion;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
