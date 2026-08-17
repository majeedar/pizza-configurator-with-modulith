import { apiFetch } from "./client";
import type { StaffAdmin, StaffCreateRequest } from "./adminTypes";

export function listStaff(token: string): Promise<StaffAdmin[]> {
  return apiFetch<StaffAdmin[]>("/api/v1/admin/users", { token });
}

export function createStaff(request: StaffCreateRequest, token: string): Promise<StaffAdmin> {
  return apiFetch<StaffAdmin>("/api/v1/admin/users", { method: "POST", body: request, token });
}

export function setStaffEnabled(employeeId: string, enabled: boolean, token: string): Promise<StaffAdmin> {
  return apiFetch<StaffAdmin>(`/api/v1/admin/users/${employeeId}/enabled`, {
    method: "PUT",
    body: { enabled },
    token,
  });
}
