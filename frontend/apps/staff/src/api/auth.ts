import { apiFetch } from "./client";
import type { StaffLoginResponse } from "./types";

export function login(username: string, password: string): Promise<StaffLoginResponse> {
  return apiFetch<StaffLoginResponse>("/api/v1/staff/login", { method: "POST", body: { username, password } });
}
