import { apiFetch } from "./client";
import type { RuleAdmin, RuleAdminRequest } from "./adminTypes";

export function listRules(token: string): Promise<RuleAdmin[]> {
  return apiFetch<RuleAdmin[]>("/api/v1/admin/rules", { token });
}

export function createRule(request: RuleAdminRequest, token: string): Promise<RuleAdmin> {
  return apiFetch<RuleAdmin>("/api/v1/admin/rules", { method: "POST", body: request, token });
}

export function updateRule(ruleId: string, request: RuleAdminRequest, token: string): Promise<RuleAdmin> {
  return apiFetch<RuleAdmin>(`/api/v1/admin/rules/${ruleId}`, { method: "PUT", body: request, token });
}
