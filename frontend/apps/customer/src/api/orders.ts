import { apiFetch } from "./client";
import type { OrderCheckoutResponse, OrderView } from "./types";

/**
 * [idempotencyKey] must be generated once per checkout attempt by the caller
 * and reused across retries of *that* attempt (agent.md §7.7/§15.1) — a
 * fresh key per call would defeat the backend's replay protection.
 */
export function createOrder(
  basketId: string,
  customNotes: string | null,
  idempotencyKey: string,
  token: string | null
): Promise<OrderCheckoutResponse> {
  return apiFetch<OrderCheckoutResponse>("/api/v1/orders", {
    method: "POST",
    body: { basketId, customNotes, fcmDeviceToken: null },
    token,
    headers: { "Idempotency-Key": idempotencyKey },
  });
}

export function fetchOrderStatus(displayNumber: string, guestAccessToken: string | null, token: string | null): Promise<OrderView> {
  const query = guestAccessToken ? `?token=${encodeURIComponent(guestAccessToken)}` : "";
  return apiFetch<OrderView>(`/api/v1/orders/${displayNumber}/status${query}`, { token });
}
