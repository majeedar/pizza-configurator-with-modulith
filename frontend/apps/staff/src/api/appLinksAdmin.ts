import { apiFetch } from "./client";
import type { AppLinkAdmin, AppLinkUpdateRequest, Audience } from "./adminTypes";

export function listAppLinks(token: string): Promise<AppLinkAdmin[]> {
  return apiFetch<AppLinkAdmin[]>("/api/v1/admin/app-links", { token });
}

export function updateAppLink(
  audience: Audience,
  request: AppLinkUpdateRequest,
  token: string
): Promise<AppLinkAdmin> {
  return apiFetch<AppLinkAdmin>(`/api/v1/admin/app-links/android/${audience.toLowerCase()}`, {
    method: "PUT",
    body: request,
    token,
  });
}
