package com.example.pizzaconfigurator.admin.api;

/** Agent.md §14.4: "audit of admin rule/price/app-link changes." */
public interface AuditLog {

    void record(AuditEntry entry);
}
