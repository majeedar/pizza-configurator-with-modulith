import { apiFetch } from "./client";
import type {
  DoughAdmin,
  DoughAdminRequest,
  IngredientAdmin,
  IngredientAdminRequest,
  PizzaAdmin,
  PizzaAdminRequest,
  RecipeLineAdmin,
  RecipeLineAdminRequest,
  SizeAdmin,
  SizeAdminRequest,
} from "./adminTypes";

export function listPizzas(token: string): Promise<PizzaAdmin[]> {
  return apiFetch<PizzaAdmin[]>("/api/v1/admin/pizzas", { token });
}

export function getPizza(pizzaId: string, token: string): Promise<PizzaAdmin> {
  return apiFetch<PizzaAdmin>(`/api/v1/admin/pizzas/${pizzaId}`, { token });
}

export function createPizza(request: PizzaAdminRequest, token: string): Promise<PizzaAdmin> {
  return apiFetch<PizzaAdmin>("/api/v1/admin/pizzas", { method: "POST", body: request, token });
}

export function updatePizza(pizzaId: string, request: PizzaAdminRequest, token: string): Promise<PizzaAdmin> {
  return apiFetch<PizzaAdmin>(`/api/v1/admin/pizzas/${pizzaId}`, { method: "PUT", body: request, token });
}

export function listIngredients(token: string): Promise<IngredientAdmin[]> {
  return apiFetch<IngredientAdmin[]>("/api/v1/admin/ingredients", { token });
}

export function createIngredient(request: IngredientAdminRequest, token: string): Promise<IngredientAdmin> {
  return apiFetch<IngredientAdmin>("/api/v1/admin/ingredients", { method: "POST", body: request, token });
}

export function updateIngredient(
  ingredientId: string,
  request: IngredientAdminRequest,
  token: string
): Promise<IngredientAdmin> {
  return apiFetch<IngredientAdmin>(`/api/v1/admin/ingredients/${ingredientId}`, {
    method: "PUT",
    body: request,
    token,
  });
}

export function listSizes(token: string): Promise<SizeAdmin[]> {
  return apiFetch<SizeAdmin[]>("/api/v1/admin/sizes", { token });
}

export function createSize(request: SizeAdminRequest, token: string): Promise<SizeAdmin> {
  return apiFetch<SizeAdmin>("/api/v1/admin/sizes", { method: "POST", body: request, token });
}

export function updateSize(sizeId: string, request: SizeAdminRequest, token: string): Promise<SizeAdmin> {
  return apiFetch<SizeAdmin>(`/api/v1/admin/sizes/${sizeId}`, { method: "PUT", body: request, token });
}

export function listDoughs(token: string): Promise<DoughAdmin[]> {
  return apiFetch<DoughAdmin[]>("/api/v1/admin/doughs", { token });
}

export function createDough(request: DoughAdminRequest, token: string): Promise<DoughAdmin> {
  return apiFetch<DoughAdmin>("/api/v1/admin/doughs", { method: "POST", body: request, token });
}

export function updateDough(doughId: string, request: DoughAdminRequest, token: string): Promise<DoughAdmin> {
  return apiFetch<DoughAdmin>(`/api/v1/admin/doughs/${doughId}`, { method: "PUT", body: request, token });
}

export function listRecipe(pizzaId: string, token: string): Promise<RecipeLineAdmin[]> {
  return apiFetch<RecipeLineAdmin[]>(`/api/v1/admin/pizzas/${pizzaId}/recipe`, { token });
}

export function addRecipeLine(
  pizzaId: string,
  request: RecipeLineAdminRequest,
  token: string
): Promise<RecipeLineAdmin> {
  return apiFetch<RecipeLineAdmin>(`/api/v1/admin/pizzas/${pizzaId}/recipe`, {
    method: "POST",
    body: request,
    token,
  });
}

export function updateRecipeLine(
  pizzaId: string,
  pizzaIngredientId: string,
  request: RecipeLineAdminRequest,
  token: string
): Promise<RecipeLineAdmin> {
  return apiFetch<RecipeLineAdmin>(`/api/v1/admin/pizzas/${pizzaId}/recipe/${pizzaIngredientId}`, {
    method: "PUT",
    body: request,
    token,
  });
}

export function removeRecipeLine(pizzaId: string, pizzaIngredientId: string, token: string): Promise<void> {
  return apiFetch<void>(`/api/v1/admin/pizzas/${pizzaId}/recipe/${pizzaIngredientId}`, {
    method: "DELETE",
    token,
  });
}
