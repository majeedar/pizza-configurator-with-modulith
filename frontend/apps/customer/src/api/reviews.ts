import { apiFetch } from "./client";
import type { ReviewOutcome, ReviewRequestView } from "./types";

export function fetchReviewStatus(configurationId: string): Promise<ReviewRequestView> {
  return apiFetch<ReviewRequestView>(`/api/v1/configurations/${configurationId}/review-status`);
}

export function acceptRecommendation(configurationId: string): Promise<ReviewOutcome> {
  return apiFetch<ReviewOutcome>(`/api/v1/configurations/${configurationId}/recommendation/accept`, {
    method: "POST",
  });
}

export function rejectRecommendation(configurationId: string): Promise<ReviewOutcome> {
  return apiFetch<ReviewOutcome>(`/api/v1/configurations/${configurationId}/recommendation/reject`, {
    method: "POST",
  });
}
