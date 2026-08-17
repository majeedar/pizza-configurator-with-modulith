import { apiFetch } from "./client";
import type { PriceAdmin, PriceAdminRequest } from "./adminTypes";

export function listPrices(token: string): Promise<PriceAdmin[]> {
  return apiFetch<PriceAdmin[]>("/api/v1/admin/prices", { token });
}

export function createPrice(request: PriceAdminRequest, token: string): Promise<PriceAdmin> {
  return apiFetch<PriceAdmin>("/api/v1/admin/prices", { method: "POST", body: request, token });
}

export function updatePrice(priceId: string, request: PriceAdminRequest, token: string): Promise<PriceAdmin> {
  return apiFetch<PriceAdmin>(`/api/v1/admin/prices/${priceId}`, { method: "PUT", body: request, token });
}
