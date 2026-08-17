package com.example.pizzaconfigurator.admin.web;

import com.example.pizzaconfigurator.admin.application.AuditEventView;
import com.example.pizzaconfigurator.admin.application.AuditLogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Agent.md §9.3/§30 — requires {@code ROLE_ADMIN}. */
@RestController
@RequestMapping("/api/v1/admin/audit")
class AuditAdminController {

    private final AuditLogService auditLogService;

    AuditAdminController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    List<AuditEventView> list() {
        return auditLogService.findAll();
    }
}
