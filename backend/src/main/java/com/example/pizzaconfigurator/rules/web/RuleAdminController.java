package com.example.pizzaconfigurator.rules.web;

import com.example.pizzaconfigurator.rules.application.RuleAdminService;
import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import com.example.pizzaconfigurator.rules.web.dto.RuleAdminRequest;
import com.example.pizzaconfigurator.rules.web.dto.RuleAdminResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/**
 * Admin CRUD for rule configuration (agent.md §7.10, §9.3) — requires
 * {@code ROLE_ADMIN} via the {@code /api/v1/admin/**} Spring Security
 * filter-chain rule (agent.md §14.2, Phase 7).
 */
@RestController
@RequestMapping("/api/v1/admin/rules")
class RuleAdminController {

    private final RuleAdminService adminService;
    private final JsonMapper jsonMapper;

    RuleAdminController(RuleAdminService adminService, JsonMapper jsonMapper) {
        this.adminService = adminService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping
    List<RuleAdminResponse> findAll() {
        return adminService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{ruleId}")
    RuleAdminResponse getRule(@PathVariable UUID ruleId) {
        return toResponse(adminService.getRule(ruleId));
    }

    @PostMapping
    ResponseEntity<RuleAdminResponse> createRule(@Valid @RequestBody RuleAdminRequest request) {
        RuleDefinition rule = adminService.createRule(
            request.ruleCode(),
            request.ruleType(),
            request.scopeType(),
            request.scopeId(),
            jsonMapper.writeValueAsString(request.parameters()),
            request.message(),
            request.active(),
            request.validFrom(),
            request.validTo());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(rule));
    }

    @PutMapping("/{ruleId}")
    RuleAdminResponse updateRule(@PathVariable UUID ruleId, @Valid @RequestBody RuleAdminRequest request) {
        RuleDefinition rule = adminService.updateRule(
            ruleId,
            request.ruleType(),
            request.scopeType(),
            request.scopeId(),
            jsonMapper.writeValueAsString(request.parameters()),
            request.message(),
            request.active(),
            request.validFrom(),
            request.validTo());
        return toResponse(rule);
    }

    @SuppressWarnings("unchecked")
    private RuleAdminResponse toResponse(RuleDefinition rule) {
        Map<String, Object> parameters = jsonMapper.readValue(rule.getParametersJson(), Map.class);
        return new RuleAdminResponse(
            rule.getRuleId(),
            rule.getRuleCode(),
            rule.getRuleType(),
            rule.getScopeType(),
            rule.getScopeId(),
            parameters,
            rule.getMessage(),
            rule.isActive(),
            rule.getVersion(),
            rule.getValidFrom(),
            rule.getValidTo());
    }
}
