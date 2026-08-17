import { apiFetch } from "./client";
import type { AuditEventAdmin } from "./adminTypes";

export function listAuditEvents(token: string): Promise<AuditEventAdmin[]> {
  return apiFetch<AuditEventAdmin[]>("/api/v1/admin/audit", { token });
}
