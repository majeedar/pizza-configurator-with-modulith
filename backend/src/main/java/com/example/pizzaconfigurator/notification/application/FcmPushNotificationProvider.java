package com.example.pizzaconfigurator.notification.application;

import com.example.pizzaconfigurator.notification.domain.NotificationChannel;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Agent.md §7.9: Firebase Cloud Messaging via the Firebase Admin SDK,
 * server-side only (§14.4 — the service-account credential never ships in
 * the native app). Agent.md §27: local/dev stubs this provider by default
 * — enforced by never initializing {@link FirebaseMessaging} when no
 * project id / service-account credential is configured, rather than a
 * separate profile-conditional bean.
 */
@Component
class FcmPushNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationProvider.class);

    private final FirebaseMessaging messaging;

    FcmPushNotificationProvider(
        @Value("${app.notification.firebase.project-id:}") String projectId,
        @Value("${app.notification.firebase.service-account-json:}") String serviceAccountJson
    ) {
        this.messaging = initialize(projectId, serviceAccountJson);
    }

    private static FirebaseMessaging initialize(String projectId, String serviceAccountJson) {
        if (projectId.isBlank() || serviceAccountJson.isBlank()) {
            log.info("FIREBASE_PROJECT_ID/FIREBASE_SERVICE_ACCOUNT_JSON not configured — push notifications will be stubbed");
            return null;
        }
        try (InputStream credentialStream = credentialsInputStream(serviceAccountJson)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialStream))
                .setProjectId(projectId)
                .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to initialize the Firebase Admin SDK — push notifications will be stubbed", e);
            return null;
        }
    }

    /** {@code FIREBASE_SERVICE_ACCOUNT_JSON} may be a filesystem path or a base64-encoded key (agent.md §24). */
    private static InputStream credentialsInputStream(String value) throws IOException {
        try {
            Path path = Path.of(value);
            if (Files.exists(path)) {
                return Files.newInputStream(path);
            }
        } catch (InvalidPathException ignored) {
            // Not a valid path on this OS — fall through and treat it as base64.
        }
        return new ByteArrayInputStream(Base64.getDecoder().decode(value));
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public NotificationSendResult send(NotificationMessage message) {
        if (messaging == null) {
            log.info("Firebase Admin SDK not configured — stubbing push to device token");
            return NotificationSendResult.stub();
        }
        try {
            Message fcmMessage = Message.builder()
                .setToken(message.recipient())
                .setNotification(Notification.builder().setTitle(message.subject()).setBody(message.body()).build())
                .build();
            String messageId = messaging.send(fcmMessage);
            return NotificationSendResult.success(messageId);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM push send failed", e);
            return NotificationSendResult.failure(e.getMessage());
        }
    }
}
