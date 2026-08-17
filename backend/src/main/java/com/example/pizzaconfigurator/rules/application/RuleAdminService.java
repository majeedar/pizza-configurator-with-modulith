package com.example.pizzaconfigurator.rules.application;

import com.example.pizzaconfigurator.admin.api.AuditEntry;
import com.example.pizzaconfigurator.admin.api.AuditLog;
import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import com.example.pizzaconfigurator.rules.domain.RuleType;
import com.example.pizzaconfigurator.rules.domain.ScopeType;
import com.example.pizzaconfigurator.rules.infrastructure.persistence.RuleRepository;
import com.example.pizzaconfigurator.shared.CurrentStaffActor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Admin write operations for rule configuration (agent.md §7.10, §9.3). Not
 * a published module API — see the CatalogAdminService precedent.
 */
@Service
@Transactional
public class RuleAdminService {

    private final RuleRepository rules;
    private final AuditLog auditLog;
    private final JsonMapper jsonMapper;

    RuleAdminService(RuleRepository rules, AuditLog auditLog, JsonMapper jsonMapper) {
        this.rules = rules;
        this.auditLog = auditLog;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public List<RuleDefinition> findAll() {
        return rules.findAll();
    }

    @Transactional(readOnly = true)
    public RuleDefinition getRule(UUID ruleId) {
        return rules.findById(ruleId).orElseThrow(() -> new RuleAdminNotFoundException(ruleId));
    }

    public RuleDefinition createRule(
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
        RuleDefinition rule = rules.save(new RuleDefinition(
            ruleCode, ruleType, scopeType, scopeId, parametersJson, message, active, validFrom, validTo));
        audit("RULE_CREATED", rule.getRuleId(), null, rule);
        return rule;
    }

    public RuleDefinition updateRule(
        UUID ruleId,
        RuleType ruleType,
        ScopeType scopeType,
        String scopeId,
        String parametersJson,
        String message,
        boolean active,
        Instant validFrom,
        Instant validTo
    ) {
        RuleDefinition rule = getRule(ruleId);
        String before = jsonMapper.writeValueAsString(rule);
        rule.update(ruleType, scopeType, scopeId, parametersJson, message, active, validFrom, validTo);
        audit("RULE_UPDATED", rule.getRuleId(), before, rule);
        return rule;
    }

    private void audit(String action, UUID ruleId, String beforeJson, RuleDefinition after) {
        auditLog.record(new AuditEntry(
            CurrentStaffActor.username(), CurrentStaffActor.role(), action, "RuleDefinition", ruleId.toString(),
            beforeJson, jsonMapper.writeValueAsString(after)));
    }
}
