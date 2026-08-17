package com.example.pizzaconfigurator.configuration.api;

import java.util.UUID;

/**
 * Published API the {@code recommendation} module (§7.11) drives once a
 * kitchen/customer decision resolves a {@code ReviewRequest} — Configuration
 * Module owns all revalidation/repricing logic itself (agent.md §7.5's
 * "does not duplicate Rule or Pricing logic" applies here too); recommendation
 * only tells it which of the three outcomes occurred.
 */
public interface ConfigurationReviewIntegration {

    /** Kitchen Accept, or a customer accepting a recommendation with no patch — revalidate/reprice as-is. */
    ConfigurationSessionView approveOriginal(UUID configurationId);

    /** A customer accepting a kitchen recommendation — apply the proposed configuration, then revalidate/reprice. */
    ConfigurationSessionView approveWithPatch(UUID configurationId, ConfigurationPatch patch);

    /** Kitchen Reject, or a customer rejecting a recommendation — terminal, no Order possible from this session. */
    ConfigurationSessionView reject(UUID configurationId);
}
