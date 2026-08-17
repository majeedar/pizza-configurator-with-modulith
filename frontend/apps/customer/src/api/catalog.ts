import { apiFetch } from "./client";
import type { ConfigurableOptions, PizzaSummary } from "./types";

export function fetchPizzas(): Promise<PizzaSummary[]> {
  return apiFetch<PizzaSummary[]>("/api/v1/catalog/pizzas");
}

export function fetchPizzaOptions(pizzaId: string): Promise<ConfigurableOptions> {
  return apiFetch<ConfigurableOptions>(`/api/v1/catalog/pizzas/${pizzaId}/options`);
}
