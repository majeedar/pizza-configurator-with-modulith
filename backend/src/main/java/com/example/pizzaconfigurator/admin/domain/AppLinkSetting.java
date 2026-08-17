package com.example.pizzaconfigurator.admin.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Agent.md §5.1/§7.10: one row per distributed native app. No stored QR
 * image — generated on demand from {@code url} (§8.6), so it never drifts
 * out of sync.
 */
@Entity
@Table(name = "app_link_setting", schema = "admin")
@EntityListeners(AuditingEntityListener.class)
public class AppLinkSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID appLinkId;

    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    private Audience audience;

    private String url;
    private boolean active;
    private String updatedBy;

    @LastModifiedDate
    private Instant updatedAt;

    protected AppLinkSetting() {
    }

    public AppLinkSetting(Platform platform, Audience audience, String url, boolean active, String updatedBy) {
        this.platform = platform;
        this.audience = audience;
        this.url = url;
        this.active = active;
        this.updatedBy = updatedBy;
    }

    public void update(String url, boolean active, String updatedBy) {
        this.url = url;
        this.active = active;
        this.updatedBy = updatedBy;
    }

    public UUID getAppLinkId() {
        return appLinkId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Audience getAudience() {
        return audience;
    }

    public String getUrl() {
        return url;
    }

    public boolean isActive() {
        return active;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
