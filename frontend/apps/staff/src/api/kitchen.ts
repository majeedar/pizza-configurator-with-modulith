import { apiFetch } from "./client";
import type { OrderView } from "./types";

export function fetchActiveOrders(token: string): Promise<OrderView[]> {
  return apiFetch<OrderView[]>("/api/v1/kitchen/orders", { token });
}

export function approveOrder(orderId: string, token: string): Promise<OrderView> {
  return apiFetch<OrderView>(`/api/v1/kitchen/orders/${orderId}/approve`, { method: "POST", token });
}

export function startOrder(orderId: string, token: string): Promise<OrderView> {
  return apiFetch<OrderView>(`/api/v1/kitchen/orders/${orderId}/start`, { method: "POST", token });
}

export function markOrderReady(orderId: string, token: string): Promise<OrderView> {
  return apiFetch<OrderView>(`/api/v1/kitchen/orders/${orderId}/ready`, { method: "POST", token });
}

export function completeOrder(orderId: string, token: string): Promise<OrderView> {
  return apiFetch<OrderView>(`/api/v1/kitchen/orders/${orderId}/complete`, { method: "POST", token });
}
