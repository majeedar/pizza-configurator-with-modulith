import { apiFetch } from "./client";
import type { ConfigurationSessionView, ExtraSelection, PriceResponse, ValidationResponse } from "./types";

export interface ConfigurationInput {
  pizzaId: string;
  sizeCode: string;
  doughCode: string;
  removedIngredients: string[];
  extras: ExtraSelection[];
  comment?: string | null;
}

export function createConfiguration(input: ConfigurationInput, token?: string | null): Promise<ConfigurationSessionView> {
  return apiFetch<ConfigurationSessionView>("/api/v1/configurations", { method: "POST", body: input, token });
}

export function updateConfiguration(
  configurationId: string,
  input: ConfigurationInput
): Promise<ConfigurationSessionView> {
  return apiFetch<ConfigurationSessionView>(`/api/v1/configurations/${configurationId}`, {
    method: "PUT",
    body: input,
  });
}

export function validateConfiguration(configurationId: string): Promise<ValidationResponse> {
  return apiFetch<ValidationResponse>(`/api/v1/configurations/${configurationId}/validate`, { method: "POST" });
}

export function priceConfiguration(configurationId: string): Promise<PriceResponse> {
  return apiFetch<PriceResponse>(`/api/v1/configurations/${configurationId}/price`, { method: "POST" });
}

export function getConfiguration(configurationId: string): Promise<ConfigurationSessionView> {
  return apiFetch<ConfigurationSessionView>(`/api/v1/configurations/${configurationId}`);
}
