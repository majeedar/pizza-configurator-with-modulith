package com.example.pizzaconfigurator.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pizzaconfigurator.notification.domain.NotificationChannel;
import com.example.pizzaconfigurator.notification.domain.NotificationRecord;
import com.example.pizzaconfigurator.notification.domain.NotificationStatus;
import com.example.pizzaconfigurator.notification.domain.NotificationType;
import com.example.pizzaconfigurator.notification.infrastructure.persistence.NotificationRecordRepository;
import com.example.pizzaconfigurator.security.api.CustomerQuery;
import com.example.pizzaconfigurator.security.api.CustomerView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Agent.md §21 Scenario J: a failure in one channel must never suppress or
 * affect the other. Exercised as a plain unit test (no Spring context, no
 * database, no real network) by driving the package-private event-listener
 * methods directly, matching the "rules must be testable without HTTP or
 * database access" bar used elsewhere in this codebase.
 */
class NotificationServiceTest {

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void pushFailureDoesNotSuppressEmailAndBothAreRecordedIndependently() {
        NotificationRecordRepository records = mock(NotificationRecordRepository.class);
        CustomerQuery customerQuery = mock(CustomerQuery.class);
        when(customerQuery.findCustomer(customerId))
            .thenReturn(Optional.of(new CustomerView(customerId, "Alice", "alice@example.com")));

        FakeProvider email = new FakeProvider(NotificationChannel.EMAIL, NotificationSendResult.success("email-1"));
        FakeProvider push = new FakeProvider(NotificationChannel.PUSH, NotificationSendResult.failure("FCM unavailable"));

        NotificationService service = new NotificationService(records, customerQuery, List.of(email, push), clock);

        service.fanOut(orderId, customerId, "device-token-123", NotificationType.ORDER_READY, "Ready", "Come get it");

        assertThat(email.attempts).isEqualTo(1);
        assertThat(push.attempts).isEqualTo(1);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(records, org.mockito.Mockito.times(2)).save(captor.capture());

        NotificationRecord emailRecord = captor.getAllValues().stream()
            .filter(r -> r.getChannel() == NotificationChannel.EMAIL).findFirst().orElseThrow();
        NotificationRecord pushRecord = captor.getAllValues().stream()
            .filter(r -> r.getChannel() == NotificationChannel.PUSH).findFirst().orElseThrow();

        assertThat(emailRecord.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(emailRecord.getRecipient()).isEqualTo("alice@example.com");
        assertThat(pushRecord.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(pushRecord.getFailureReason()).isEqualTo("FCM unavailable");
    }

    @Test
    void guestOrderWithNoFcmTokenAttemptsNoChannels() {
        NotificationRecordRepository records = mock(NotificationRecordRepository.class);
        CustomerQuery customerQuery = mock(CustomerQuery.class);
        FakeProvider email = new FakeProvider(NotificationChannel.EMAIL, NotificationSendResult.success("x"));
        FakeProvider push = new FakeProvider(NotificationChannel.PUSH, NotificationSendResult.success("y"));

        NotificationService service = new NotificationService(records, customerQuery, List.of(email, push), clock);

        service.fanOut(orderId, null, null, NotificationType.ORDER_STATUS_UPDATE, "Confirmed", "Thanks");

        assertThat(email.attempts).isZero();
        assertThat(push.attempts).isZero();
        verify(records, org.mockito.Mockito.never()).save(any());
    }

    private static final class FakeProvider implements NotificationProvider {
        private final NotificationChannel channel;
        private final NotificationSendResult result;
        int attempts;

        FakeProvider(NotificationChannel channel, NotificationSendResult result) {
            this.channel = channel;
            this.result = result;
        }

        @Override
        public NotificationChannel channel() {
            return channel;
        }

        @Override
        public NotificationSendResult send(NotificationMessage message) {
            attempts++;
            return result;
        }
    }
}
