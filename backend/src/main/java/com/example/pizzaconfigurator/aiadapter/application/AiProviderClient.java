package com.example.pizzaconfigurator.aiadapter.application;

import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;

/**
 * One implementation per vendor (agent.md §7.4). Throws {@link
 * AiProviderException} — never propagates a raw HTTP/parsing exception —
 * so {@link CommentInterpreterService} can fail over uniformly regardless
 * of provider.
 */
interface AiProviderClient {

    String name();

    RawParsedComment interpret(String comment, ConfigurableOptions allowedOptions);
}
