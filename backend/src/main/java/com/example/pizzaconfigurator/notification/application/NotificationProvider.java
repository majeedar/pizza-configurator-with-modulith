package com.example.pizzaconfigurator.notification.application;

import com.example.pizzaconfigurator.notification.domain.NotificationChannel;

/**
 * Agent.md §7.9: adding, swapping, or removing a channel is a new
 * implementation class only, no redesign of the module.
 */
public interface NotificationProvider {

    NotificationChannel channel();

    NotificationSendResult send(NotificationMessage message);
}
