import { apiFetch } from "./client";
import type { ConfigurationPatchRequest, ReviewRequestView } from "./types";

export function fetchReviewQueue(token: string): Promise<ReviewRequestView[]> {
  return apiFetch<ReviewRequestView[]>("/api/v1/kitchen/reviews", { token });
}

export function acceptReview(reviewRequestId: string, token: string): Promise<{ reviewRequest: ReviewRequestView }> {
  return apiFetch(`/api/v1/kitchen/reviews/${reviewRequestId}/accept`, { method: "POST", token });
}

export function recommendReview(
  reviewRequestId: string,
  patch: ConfigurationPatchRequest,
  token: string
): Promise<{ reviewRequest: ReviewRequestView }> {
  return apiFetch(`/api/v1/kitchen/reviews/${reviewRequestId}/recommend`, { method: "POST", body: patch, token });
}

export function rejectReview(
  reviewRequestId: string,
  reason: string | null,
  token: string
): Promise<{ reviewRequest: ReviewRequestView }> {
  return apiFetch(`/api/v1/kitchen/reviews/${reviewRequestId}/reject`, {
    method: "POST",
    body: reason ? { reason } : undefined,
    token,
  });
}
