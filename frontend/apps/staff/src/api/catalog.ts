import { apiFetch } from "./client";
import type { ConfigurableOptions } from "./types";

// The public catalog endpoint (not /api/v1/admin/**) — used for the kitchen
// review queue's "Recommend" form, which ROLE_KITCHEN staff (no admin
// access) also need to be able to fill in.
export function fetchPizzaOptions(pizzaId: string): Promise<ConfigurableOptions> {
  return apiFetch<ConfigurableOptions>(`/api/v1/catalog/pizzas/${pizzaId}/options`);
}
