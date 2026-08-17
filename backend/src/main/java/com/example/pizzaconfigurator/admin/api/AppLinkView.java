package com.example.pizzaconfigurator.admin.api;

import com.example.pizzaconfigurator.admin.domain.Audience;
import com.example.pizzaconfigurator.admin.domain.Platform;
import java.time.Instant;
import java.util.UUID;

public record AppLinkView(UUID appLinkId, Platform platform, Audience audience, String url, boolean active, String updatedBy, Instant updatedAt) {
}
