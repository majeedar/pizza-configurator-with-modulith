package com.example.pizzaconfigurator.admin.application;

import com.example.pizzaconfigurator.admin.api.AppLinkQuery;
import com.example.pizzaconfigurator.admin.api.AppLinkView;
import com.example.pizzaconfigurator.admin.api.AuditEntry;
import com.example.pizzaconfigurator.admin.api.AuditLog;
import com.example.pizzaconfigurator.admin.domain.AppLinkSetting;
import com.example.pizzaconfigurator.admin.domain.Audience;
import com.example.pizzaconfigurator.admin.domain.Platform;
import com.example.pizzaconfigurator.admin.infrastructure.persistence.AppLinkSettingRepository;
import com.example.pizzaconfigurator.shared.CurrentStaffActor;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Agent.md §7.10: manages {@code AppLinkSetting} rows and exposes {@link AppLinkQuery} for other modules/frontends. */
@Service
@Transactional
public class AppLinkService implements AppLinkQuery {

    private final AppLinkSettingRepository appLinks;
    private final AuditLog auditLog;
    private final JsonMapper jsonMapper;

    AppLinkService(AppLinkSettingRepository appLinks, AuditLog auditLog, JsonMapper jsonMapper) {
        this.appLinks = appLinks;
        this.auditLog = auditLog;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppLinkView> getActiveLink(Platform platform, Audience audience) {
        return appLinks.findByPlatformAndAudience(platform, audience).filter(AppLinkSetting::isActive).map(this::toView);
    }

    @Transactional(readOnly = true)
    public List<AppLinkView> findAll() {
        return appLinks.findAllByOrderByAudienceAsc().stream().map(this::toView).toList();
    }

    /** Agent.md §8.6: one entry per audience — created on first use, updated thereafter. */
    public AppLinkView upsert(Platform platform, Audience audience, String url, boolean active) {
        String actor = CurrentStaffActor.username();
        AppLinkSetting setting = appLinks.findByPlatformAndAudience(platform, audience).orElse(null);
        String beforeJson = setting == null ? null : jsonMapper.writeValueAsString(toView(setting));

        if (setting == null) {
            setting = appLinks.save(new AppLinkSetting(platform, audience, url, active, actor));
        } else {
            setting.update(url, active, actor);
        }

        AppLinkView after = toView(setting);
        auditLog.record(new AuditEntry(
            actor, "ADMIN", "APP_LINK_UPDATED", "AppLinkSetting", setting.getAppLinkId().toString(),
            beforeJson, jsonMapper.writeValueAsString(after)));
        return after;
    }

    private AppLinkView toView(AppLinkSetting setting) {
        return new AppLinkView(
            setting.getAppLinkId(), setting.getPlatform(), setting.getAudience(), setting.getUrl(),
            setting.isActive(), setting.getUpdatedBy(), setting.getUpdatedAt());
    }
}
