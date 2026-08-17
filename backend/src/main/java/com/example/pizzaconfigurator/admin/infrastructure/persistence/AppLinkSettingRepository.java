package com.example.pizzaconfigurator.admin.infrastructure.persistence;

import com.example.pizzaconfigurator.admin.domain.AppLinkSetting;
import com.example.pizzaconfigurator.admin.domain.Audience;
import com.example.pizzaconfigurator.admin.domain.Platform;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppLinkSettingRepository extends JpaRepository<AppLinkSetting, UUID> {

    Optional<AppLinkSetting> findByPlatformAndAudience(Platform platform, Audience audience);

    List<AppLinkSetting> findAllByOrderByAudienceAsc();
}
