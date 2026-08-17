import { apiFetch } from "./client";
import type { AuthResponse } from "./types";

export function register(name: string, email: string, phoneNumber: string | null, password: string): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/api/v1/customers/register", {
    method: "POST",
    body: { name, email, phoneNumber, password },
  });
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/api/v1/customers/login", { method: "POST", body: { email, password } });
}
