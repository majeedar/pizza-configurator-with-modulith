package com.example.pizzaconfigurator.rules.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * The database stores rule configuration; the Java evaluators (see
 * {@code rules.domain.evaluator}) contain the deterministic logic. This
 * entity never carries executable code, only typed parameters (agent.md
 * §5.1, §11).
 */
@Entity
@Table(name = "rule_definition", schema = "rules")
@EntityListeners(AuditingEntityListener.class)
public class RuleDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ruleId;

    private String ruleCode;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    private ScopeType scopeType;

    private String scopeId;

    private String parametersJson;

    private String message;

    private boolean active;

    @Version
    private Long version;

    private Instant validFrom;
    private Instant validTo;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected RuleDefinition() {
    }

    public RuleDefinition(
        String ruleCode,
        RuleType ruleType,
        ScopeType scopeType,
        String scopeId,
        String parametersJson,
        String message,
        boolean active,
        Instant validFrom,
        Instant validTo
    ) {
        this.ruleCode = ruleCode;
        this.ruleType = ruleType;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.parametersJson = parametersJson;
        this.message = message;
        this.active = active;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public boolean appliesTo(String pizzaCode) {
        return scopeType == ScopeType.GLOBAL || scopeId.equals(pizzaCode);
    }

    public boolean isCurrentlyValid(Instant now) {
        return (validFrom == null || !now.isBefore(validFrom)) && (validTo == null || !now.isAfter(validTo));
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public String getMessage() {
        return message;
    }

    public boolean isActive() {
        return active;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void update(
        RuleType ruleType,
        ScopeType scopeType,
        String scopeId,
        String parametersJson,
        String message,
        boolean active,
        Instant validFrom,
        Instant validTo
    ) {
        this.ruleType = ruleType;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.parametersJson = parametersJson;
        this.message = message;
        this.active = active;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
}
