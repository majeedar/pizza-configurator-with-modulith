package com.example.pizzaconfigurator.admin.web;

import com.example.pizzaconfigurator.admin.api.AppLinkNotFoundException;
import com.example.pizzaconfigurator.admin.api.AppLinkQuery;
import com.example.pizzaconfigurator.admin.api.AppLinkView;
import com.example.pizzaconfigurator.admin.application.QrCodeGenerator;
import com.example.pizzaconfigurator.admin.domain.Audience;
import com.example.pizzaconfigurator.admin.domain.Platform;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent.md §8.6/§9.1: the "Get the app" link/QR, public — no auth, guest
 * and logged-in customers alike.
 */
@RestController
@RequestMapping("/api/v1/app-links/android")
class PublicAppLinkController {

    private final AppLinkQuery appLinkQuery;
    private final QrCodeGenerator qrCodeGenerator;

    PublicAppLinkController(AppLinkQuery appLinkQuery, QrCodeGenerator qrCodeGenerator) {
        this.appLinkQuery = appLinkQuery;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    @GetMapping("/{audience}")
    AppLinkView get(@PathVariable String audience) {
        return activeLink(audience);
    }

    @GetMapping(value = "/{audience}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> qr(@PathVariable String audience) {
        AppLinkView link = activeLink(audience);
        byte[] png = qrCodeGenerator.generatePng(link.url());
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(png);
    }

    private AppLinkView activeLink(String audience) {
        Audience parsed = AudienceParser.parse(audience);
        return appLinkQuery.getActiveLink(Platform.ANDROID, parsed).orElseThrow(() -> new AppLinkNotFoundException(parsed));
    }
}
