package com.example.pizzaconfigurator.admin.api;

import com.example.pizzaconfigurator.admin.domain.Audience;

public class AppLinkNotFoundException extends RuntimeException {

    public AppLinkNotFoundException(Audience audience) {
        super("No active app link configured for audience " + audience);
    }
}
