package com.example.pizzaconfigurator.admin.web;

import com.example.pizzaconfigurator.admin.api.AppLinkView;
import com.example.pizzaconfigurator.admin.application.AppLinkService;
import com.example.pizzaconfigurator.admin.domain.Platform;
import com.example.pizzaconfigurator.admin.web.dto.AppLinkUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Agent.md §9.3/§7.10 — requires {@code ROLE_ADMIN} via the existing {@code /api/v1/admin/**} filter-chain rule. */
@RestController
@RequestMapping("/api/v1/admin/app-links")
class AppLinkAdminController {

    private final AppLinkService appLinkService;

    AppLinkAdminController(AppLinkService appLinkService) {
        this.appLinkService = appLinkService;
    }

    @GetMapping
    List<AppLinkView> list() {
        return appLinkService.findAll();
    }

    @PutMapping("/android/{audience}")
    AppLinkView update(@PathVariable String audience, @Valid @RequestBody AppLinkUpdateRequest request) {
        return appLinkService.upsert(Platform.ANDROID, AudienceParser.parse(audience), request.url(), request.active());
    }
}
