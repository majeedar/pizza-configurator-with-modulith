/**
 * Order status/ready notifications, fanned out per channel (email via Gmail
 * SMTP, push via Firebase Cloud Messaging). Channels fail independently and
 * never roll back an Order.
 */
package com.example.pizzaconfigurator.notification;
