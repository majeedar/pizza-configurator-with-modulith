import { apiBaseUrl, apiFetch } from "./client";
import type { ConfigurableOptions, PizzaSummary } from "./types";

export function fetchPizzas(): Promise<PizzaSummary[]> {
  return apiFetch<PizzaSummary[]>("/api/v1/catalog/pizzas");
}

export function fetchPizzaOptions(pizzaId: string): Promise<ConfigurableOptions> {
  return apiFetch<ConfigurableOptions>(`/api/v1/catalog/pizzas/${pizzaId}/options`);
}

// Pizza photos are served from the backend (not baked into the frontend
// bundle) via a relative path, e.g. "/api/v1/catalog/pizzas/{id}/image" — it
// needs the same base URL apiFetch uses under the hood, since a plain <img
// src> doesn't go through apiFetch.
export function resolveImageUrl(imageUrl: string | null): string | null {
  return imageUrl ? `${apiBaseUrl()}${imageUrl}` : null;
}
