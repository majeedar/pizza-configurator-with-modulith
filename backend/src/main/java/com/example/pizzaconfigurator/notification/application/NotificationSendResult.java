package com.example.pizzaconfigurator.notification.application;

/**
 * A provider must never throw — always report success/failure here so one
 * channel's failure can never affect another (agent.md §7.9). A "stub"
 * result (no real credentials configured, agent.md §27 local default) is
 * reported as a successful send with a synthetic message id, so local/dev
 * flows complete the same way a real send would.
 */
public record NotificationSendResult(boolean success, String providerMessageId, String failureReason) {

    public static NotificationSendResult success(String providerMessageId) {
        return new NotificationSendResult(true, providerMessageId, null);
    }

    public static NotificationSendResult failure(String failureReason) {
        return new NotificationSendResult(false, null, failureReason);
    }

    public static NotificationSendResult stub() {
        return success("stub-not-configured");
    }
}
