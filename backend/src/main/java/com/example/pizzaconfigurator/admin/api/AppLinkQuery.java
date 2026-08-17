package com.example.pizzaconfigurator.admin.api;

import com.example.pizzaconfigurator.admin.domain.Audience;
import com.example.pizzaconfigurator.admin.domain.Platform;
import java.util.Optional;

/** Agent.md §7.10: lets other modules/frontends read the active link/QR without reaching into admin's persistence. */
public interface AppLinkQuery {

    Optional<AppLinkView> getActiveLink(Platform platform, Audience audience);
}
