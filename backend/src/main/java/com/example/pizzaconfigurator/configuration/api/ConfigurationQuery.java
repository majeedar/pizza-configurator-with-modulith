package com.example.pizzaconfigurator.configuration.api;

import java.util.UUID;

/**
 * Published read API of the configuration module — used by Basket (agent.md
 * §7.6) to snapshot a priced, valid configuration when it's added.
 */
public interface ConfigurationQuery {

    ConfigurationSessionView getSession(UUID configurationId);
}
