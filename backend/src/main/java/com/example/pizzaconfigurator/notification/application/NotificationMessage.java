package com.example.pizzaconfigurator.notification.application;

/**
 * Generic enough for both channels: for email, {@code recipient} is an
 * address; for push, it's an FCM device token and {@code subject}/{@code
 * body} become the notification title/body (agent.md §7.9).
 */
public record NotificationMessage(String recipient, String subject, String body) {
}
