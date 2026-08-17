package com.example.pizzaconfigurator.admin.web;

import com.example.pizzaconfigurator.admin.api.AppLinkNotFoundException;
import com.example.pizzaconfigurator.admin.api.AppLinkQuery;
import com.example.pizzaconfigurator.admin.api.AppLinkView;
import com.example.pizzaconfigurator.admin.domain.Audience;
import com.example.pizzaconfigurator.admin.domain.Platform;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent.md §8.2/§9.2: the customer app's QR code shown on the KDS
 * production board, for staff to point in-store customers at — requires
 * {@code ROLE_KITCHEN}/{@code ROLE_ADMIN} via the existing
 * {@code /api/v1/kitchen/**} filter-chain rule.
 */
@RestController
@RequestMapping("/api/v1/kitchen/app-links/android")
class KitchenAppLinkController {

    private final AppLinkQuery appLinkQuery;

    KitchenAppLinkController(AppLinkQuery appLinkQuery) {
        this.appLinkQuery = appLinkQuery;
    }

    @GetMapping("/customer")
    AppLinkView customerLink() {
        return appLinkQuery.getActiveLink(Platform.ANDROID, Audience.CUSTOMER)
            .orElseThrow(() -> new AppLinkNotFoundException(Audience.CUSTOMER));
    }
}
