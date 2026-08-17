package com.example.pizzaconfigurator.notification.application;

import com.example.pizzaconfigurator.notification.domain.NotificationChannel;
import com.example.pizzaconfigurator.notification.domain.NotificationRecord;
import com.example.pizzaconfigurator.notification.domain.NotificationType;
import com.example.pizzaconfigurator.notification.infrastructure.persistence.NotificationRecordRepository;
import com.example.pizzaconfigurator.orders.api.OrderApproved;
import com.example.pizzaconfigurator.orders.api.OrderCompleted;
import com.example.pizzaconfigurator.orders.api.OrderPlaced;
import com.example.pizzaconfigurator.orders.api.OrderProcessingStarted;
import com.example.pizzaconfigurator.orders.api.OrderReady;
import com.example.pizzaconfigurator.security.api.CustomerQuery;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Agent.md §7.9: fans an order lifecycle event out to every channel with
 * data available for that order — email when the customer has one
 * (registered customers only; guest orders have no captured email address),
 * push when {@code Order.fcmDeviceToken} is set — one {@link
 * NotificationRecord} per channel attempted, independently. Listens {@code
 * AFTER_COMMIT} so a notification is never attempted for an order that
 * might still roll back, and a provider failure can never roll the Order
 * back either way.
 *
 * <p>Each listener method is {@code @Transactional(REQUIRES_NEW)} — not an
 * arbitrary choice: an {@code AFTER_COMMIT} callback runs after the
 * triggering transaction's connection/synchronization state has already
 * started tearing down, so letting {@code NotificationRecordRepository.save}
 * participate in whatever transactional context happens to still be
 * lingering on the thread (the default {@code REQUIRED} propagation) is
 * unreliable — Hibernate hands back a generated id as if the save
 * succeeded, but the row is never actually durably committed. Spring
 * enforces the same rule at the framework level for {@code @Transactional}
 * directly on a {@code @TransactionalEventListener} method (REQUIRES_NEW or
 * NOT_SUPPORTED only); this is that exact scenario one level down the call
 * stack.
 */
@Component
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRecordRepository records;
    private final CustomerQuery customerQuery;
    private final Map<NotificationChannel, NotificationProvider> providers;
    private final Clock clock;

    NotificationService(NotificationRecordRepository records, CustomerQuery customerQuery, List<NotificationProvider> providers, Clock clock) {
        this.records = records;
        this.customerQuery = customerQuery;
        this.providers = new EnumMap<>(NotificationChannel.class);
        providers.forEach(provider -> this.providers.put(provider.channel(), provider));
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onOrderPlaced(OrderPlaced event) {
        fanOut(event.orderId(), event.customerId(), event.fcmDeviceToken(), NotificationType.ORDER_STATUS_UPDATE,
            "Order " + event.displayNumber() + " confirmed", "We've received your order " + event.displayNumber() + ".");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onOrderApproved(OrderApproved event) {
        fanOut(event.orderId(), event.customerId(), event.fcmDeviceToken(), NotificationType.ORDER_STATUS_UPDATE,
            "Order " + event.displayNumber() + " approved", "Your order " + event.displayNumber() + " has been approved by the kitchen.");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onOrderProcessingStarted(OrderProcessingStarted event) {
        fanOut(event.orderId(), event.customerId(), event.fcmDeviceToken(), NotificationType.ORDER_STATUS_UPDATE,
            "Order " + event.displayNumber() + " is being prepared", "The kitchen has started preparing order " + event.displayNumber() + ".");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onOrderReady(OrderReady event) {
        fanOut(event.orderId(), event.customerId(), event.fcmDeviceToken(), NotificationType.ORDER_READY,
            "Order " + event.displayNumber() + " is ready", "Order " + event.displayNumber() + " is ready for pickup.");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onOrderCompleted(OrderCompleted event) {
        fanOut(event.orderId(), event.customerId(), event.fcmDeviceToken(), NotificationType.ORDER_STATUS_UPDATE,
            "Order " + event.displayNumber() + " completed", "Thanks for your order, " + event.displayNumber() + "!");
    }

    void fanOut(UUID orderId, UUID customerId, String fcmDeviceToken, NotificationType type, String subject, String body) {
        if (customerId != null) {
            customerQuery.findCustomer(customerId)
                .ifPresent(customer -> attempt(orderId, NotificationChannel.EMAIL, customer.email(), type, subject, body));
        }
        if (fcmDeviceToken != null && !fcmDeviceToken.isBlank()) {
            attempt(orderId, NotificationChannel.PUSH, fcmDeviceToken, type, subject, body);
        }
    }

    private void attempt(UUID orderId, NotificationChannel channel, String recipient, NotificationType type, String subject, String body) {
        NotificationProvider provider = providers.get(channel);
        NotificationSendResult result = provider.send(new NotificationMessage(recipient, subject, body));
        NotificationRecord record = result.success()
            ? NotificationRecord.sent(orderId, channel, recipient, type, result.providerMessageId(), clock)
            : NotificationRecord.failed(orderId, channel, recipient, type, result.failureReason());
        records.save(record);
        if (!result.success()) {
            log.warn("Notification channel {} failed for order {}: {}", channel, orderId, result.failureReason());
        }
    }
}
