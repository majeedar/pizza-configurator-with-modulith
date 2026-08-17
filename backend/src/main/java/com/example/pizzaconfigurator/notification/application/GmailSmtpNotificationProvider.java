package com.example.pizzaconfigurator.notification.application;

import com.example.pizzaconfigurator.notification.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Agent.md §7.9: Gmail SMTP ({@code smtp.gmail.com:587}, STARTTLS, Gmail
 * App Password) — a "for now" choice, not meant for production-scale
 * transactional email (§31/§7.9 free-tier cap). Agent.md §27: local/dev
 * stubs this provider by default — enforced here by simply never calling
 * {@link JavaMailSender#send} when no username is configured, rather than
 * a separate profile-conditional bean.
 */
@Component
class GmailSmtpNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(GmailSmtpNotificationProvider.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    GmailSmtpNotificationProvider(JavaMailSender mailSender, @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationSendResult send(NotificationMessage message) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.info("GMAIL_SMTP_USERNAME not configured — stubbing email to {}", message.recipient());
            return NotificationSendResult.stub();
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(message.recipient());
            mail.setSubject(message.subject());
            mail.setText(message.body());
            mailSender.send(mail);
            return NotificationSendResult.success(null);
        } catch (MailException e) {
            log.warn("Gmail SMTP send to {} failed", message.recipient(), e);
            return NotificationSendResult.failure(e.getMessage());
        }
    }
}
