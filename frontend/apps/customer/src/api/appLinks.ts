import { apiBaseUrl, apiFetch } from "./client";
import type { AppLinkView } from "./types";

export function fetchCustomerAppLink(): Promise<AppLinkView> {
  return apiFetch<AppLinkView>("/api/v1/app-links/android/customer");
}

export function customerAppQrUrl(): string {
  return `${apiBaseUrl()}/api/v1/app-links/android/customer/qr.png`;
}
