import { apiFetch } from "./client";
import type { BasketView } from "./types";

export function createBasket(token?: string | null): Promise<BasketView> {
  return apiFetch<BasketView>("/api/v1/baskets", { method: "POST", token });
}

export function getBasket(basketId: string): Promise<BasketView> {
  return apiFetch<BasketView>(`/api/v1/baskets/${basketId}`);
}

export function addBasketItem(basketId: string, configurationId: string, quantity: number): Promise<BasketView> {
  return apiFetch<BasketView>(`/api/v1/baskets/${basketId}/items`, {
    method: "POST",
    body: { configurationId, quantity },
  });
}

export function removeBasketItem(basketId: string, basketItemId: string): Promise<BasketView> {
  return apiFetch<BasketView>(`/api/v1/baskets/${basketId}/items/${basketItemId}`, { method: "DELETE" });
}
