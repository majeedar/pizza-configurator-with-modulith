import { apiBaseUrl, apiFetch } from "./client";
import type { ConfigurableOptions, ExtraConstraints, PizzaSummary } from "./types";

export function fetchPizzas(): Promise<PizzaSummary[]> {
  return apiFetch<PizzaSummary[]>("/api/v1/catalog/pizzas");
}

export function fetchPizzaOptions(pizzaId: string): Promise<ConfigurableOptions> {
  return apiFetch<ConfigurableOptions>(`/api/v1/catalog/pizzas/${pizzaId}/options`);
}

// Owned by the rules module, registered under this customer-facing catalog
// path (see the backend's ExtraConstraintsController) — which extras aren't
// offered on this pizza, and the max quantity for the ones that are capped.
export function fetchExtraConstraints(pizzaId: string): Promise<ExtraConstraints> {
  return apiFetch<ExtraConstraints>(`/api/v1/catalog/pizzas/${pizzaId}/extra-constraints`);
}

// Pizza photos are served from the backend (not baked into the frontend
// bundle) via a relative path, e.g. "/api/v1/catalog/pizzas/{id}/image" — it
// needs the same base URL apiFetch uses under the hood, since a plain <img
// src> doesn't go through apiFetch.
export function resolveImageUrl(imageUrl: string | null): string | null {
  return imageUrl ? `${apiBaseUrl()}${imageUrl}` : null;
}
