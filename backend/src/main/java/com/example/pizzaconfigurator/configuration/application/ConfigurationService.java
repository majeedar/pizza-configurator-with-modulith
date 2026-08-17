package com.example.pizzaconfigurator.configuration.application;

import com.example.pizzaconfigurator.aiadapter.api.CommentInterpretationRequest;
import com.example.pizzaconfigurator.aiadapter.api.CommentInterpreter;
import com.example.pizzaconfigurator.aiadapter.api.ParsedComment;
import com.example.pizzaconfigurator.aiadapter.api.ParsedExtra;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.PizzaView;
import com.example.pizzaconfigurator.configuration.api.ConfigurationNotFoundException;
import com.example.pizzaconfigurator.configuration.api.ConfigurationPatch;
import com.example.pizzaconfigurator.configuration.api.ConfigurationQuery;
import com.example.pizzaconfigurator.configuration.api.ConfigurationReviewIntegration;
import com.example.pizzaconfigurator.configuration.api.ConfigurationReviewRequested;
import com.example.pizzaconfigurator.configuration.api.ConfigurationSessionView;
import com.example.pizzaconfigurator.configuration.api.SelectionExtra;
import com.example.pizzaconfigurator.configuration.domain.ConfigurationSession;
import com.example.pizzaconfigurator.configuration.domain.SelectionSnapshot;
import com.example.pizzaconfigurator.configuration.domain.ValidationStatus;
import com.example.pizzaconfigurator.configuration.infrastructure.persistence.ConfigurationSessionRepository;
import com.example.pizzaconfigurator.pricing.api.PriceQuote;
import com.example.pizzaconfigurator.pricing.api.PricedExtra;
import com.example.pizzaconfigurator.pricing.api.PricingService;
import com.example.pizzaconfigurator.pricing.api.ValidatedConfiguration;
import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import com.example.pizzaconfigurator.rules.api.ExtraSelection;
import com.example.pizzaconfigurator.rules.api.RuleValidation;
import com.example.pizzaconfigurator.rules.api.ValidationResult;
import com.example.pizzaconfigurator.rules.api.Violation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * The application-flow coordinator (agent.md §7.5): combines customer
 * selections with catalog data, invokes Rules, invokes Pricing after a
 * successful validation, and maintains the ConfigurationSession lifecycle.
 * Does not duplicate Rule or Pricing logic.
 *
 * <p>Comment-driven configuration (agent.md §7.4/§13): a non-blank comment
 * is sent to the AI Adapter, merged with the customer's structured UI
 * selections on success, and validated through the Rule Module exactly
 * like the no-comment path. Any AI ambiguity/failure is honestly parked as
 * {@link ValidationStatus#PENDING_REVIEW} rather than silently ignored or
 * guessed at.
 *
 * <p>Also implements {@link ConfigurationReviewIntegration} (agent.md
 * §7.11): the {@code recommendation} module drives revalidation/repricing
 * through this same instance once a kitchen/customer decision resolves a
 * {@code ReviewRequest} it owns — Configuration Module never needs to know
 * {@code ReviewRequest} exists.
 */
@Service
@Transactional
public class ConfigurationService implements ConfigurationQuery, ConfigurationReviewIntegration {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final ConfigurationSessionRepository sessions;
    private final CatalogQuery catalogQuery;
    private final RuleValidation ruleValidation;
    private final PricingService pricingService;
    private final CommentInterpreter commentInterpreter;
    private final ApplicationEventPublisher events;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    ConfigurationService(
        ConfigurationSessionRepository sessions,
        CatalogQuery catalogQuery,
        RuleValidation ruleValidation,
        PricingService pricingService,
        CommentInterpreter commentInterpreter,
        ApplicationEventPublisher events,
        JsonMapper jsonMapper,
        Clock clock
    ) {
        this.sessions = sessions;
        this.catalogQuery = catalogQuery;
        this.ruleValidation = ruleValidation;
        this.pricingService = pricingService;
        this.commentInterpreter = commentInterpreter;
        this.events = events;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigurationSessionView getSession(UUID configurationId) {
        return toView(getEntity(configurationId));
    }

    public ConfigurationSessionView createSession(
        UUID customerId, UUID pizzaId, String sizeCode, String doughCode,
        Set<String> removedIngredientCodes, List<SelectionSnapshot.Extra> extras, String comment
    ) {
        catalogQuery.getPizza(pizzaId); // throws PizzaNotFoundException if unknown
        String json = serialize(new SelectionSnapshot(removedIngredientCodes, extras));
        Instant expiresAt = Instant.now(clock).plus(SESSION_TTL);
        ConfigurationSession session = new ConfigurationSession(
            customerId, pizzaId, sizeCode, doughCode, json, comment, expiresAt);
        return toView(sessions.save(session));
    }

    public ConfigurationSessionView updateSession(
        UUID configurationId, UUID pizzaId, String sizeCode, String doughCode,
        Set<String> removedIngredientCodes, List<SelectionSnapshot.Extra> extras, String comment
    ) {
        ConfigurationSession session = getEntity(configurationId);
        catalogQuery.getPizza(pizzaId);
        String json = serialize(new SelectionSnapshot(removedIngredientCodes, extras));
        session.updateSelections(pizzaId, sizeCode, doughCode, json, comment);
        return toView(session);
    }

    public ValidationOutcome validateSession(UUID configurationId) {
        ConfigurationSession session = getEntity(configurationId);
        requireNotExpired(session);
        SelectionSnapshot snapshot = deserialize(session.getConfigurationJson());

        Set<String> removedIngredientCodes = snapshot.removedIngredientCodes();
        List<SelectionSnapshot.Extra> extras = snapshot.extras();
        String sizeCode = session.getSizeCode();
        String doughCode = session.getDoughCode();

        // Agent.md §13.1: only ever call AI for a non-blank comment.
        if (session.getComment() != null && !session.getComment().isBlank()) {
            ParsedComment parsed = commentInterpreter.interpret(
                new CommentInterpretationRequest(session.getPizzaId(), session.getComment()));

            // §13.3: any AI failure/ambiguity (timeout, error, malformed
            // response, unknown ingredient, unresolved language) — never
            // guess, park for manual review instead.
            if (!parsed.isFullyResolved()) {
                session.markValidated(ValidationStatus.PENDING_REVIEW, null);
                String reason = "AI could not fully interpret comment: " + String.join("; ", parsed.unresolvedText());
                events.publishEvent(new ConfigurationReviewRequested(
                    configurationId, reason, originalRequestJson(session, removedIngredientCodes, extras, sizeCode, doughCode)));
                return new ValidationOutcome(toView(session), List.of(), List.of());
            }

            // §13.2 step 4: merge with the structured UI configuration. The
            // comment reflects the customer's most recent explicit intent,
            // so an AI-requested extra quantity or size/dough overrides the
            // UI's; removals/extras the comment doesn't mention are left as
            // the customer set them via the UI.
            removedIngredientCodes = union(removedIngredientCodes, parsed.removeIngredientCodes());
            extras = mergeExtras(extras, parsed.extras());
            sizeCode = parsed.requestedSizeCode() != null ? parsed.requestedSizeCode() : sizeCode;
            doughCode = parsed.requestedDoughCode() != null ? parsed.requestedDoughCode() : doughCode;

            // The merged result becomes the session's configuration from
            // here on — priceSession() prices whatever is currently stored,
            // so the AI-resolved selections must be persisted, not just
            // used for this one validation call.
            session.updateSelections(
                session.getPizzaId(), sizeCode, doughCode,
                serialize(new SelectionSnapshot(removedIngredientCodes, extras)), session.getComment());
        }

        // §13.2 step 5: send the merged candidate through the Rule Module —
        // identical path to the no-comment case. The AI result is never
        // trusted merely because it parsed; only a VALID verdict here
        // matters.
        ConfigurationCandidate candidate = new ConfigurationCandidate(
            session.getPizzaId(),
            sizeCode,
            doughCode,
            removedIngredientCodes,
            extras.stream().map(e -> new ExtraSelection(e.ingredientCode(), e.quantity())).toList());

        ValidationResult result = ruleValidation.validate(candidate);
        boolean isValid = result.status() == com.example.pizzaconfigurator.rules.api.ValidationStatus.VALID;

        // §4.3 steps 1-3: the Rule Module's own deterministic suggestions
        // (already computed above) are step 1's "attempt deterministic
        // recommendation" — if any exist, return them to the customer to
        // self-correct (step 2), same as before Phase 10. Only when INVALID
        // with *no* suggestion at all does this become a manual ReviewRequest.
        if (isValid) {
            session.markValidated(ValidationStatus.VALID, result.ruleVersion());
        } else if (result.suggestions().isEmpty()) {
            session.markValidated(ValidationStatus.PENDING_REVIEW, null);
            String reason = "No deterministic suggestion available: "
                + result.violations().stream().map(Violation::message).collect(Collectors.joining("; "));
            events.publishEvent(new ConfigurationReviewRequested(
                configurationId, reason, originalRequestJson(session, removedIngredientCodes, extras, sizeCode, doughCode)));
        } else {
            session.markValidated(ValidationStatus.INVALID, result.ruleVersion());
        }

        return new ValidationOutcome(toView(session), result.violations(), result.suggestions());
    }

    private String originalRequestJson(
        ConfigurationSession session, Set<String> removedIngredientCodes, List<SelectionSnapshot.Extra> extras,
        String sizeCode, String doughCode
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("pizzaId", session.getPizzaId().toString());
        snapshot.put("sizeCode", sizeCode);
        snapshot.put("doughCode", doughCode);
        snapshot.put("removedIngredientCodes", removedIngredientCodes);
        snapshot.put("extras", extras);
        snapshot.put("comment", session.getComment());
        return jsonMapper.writeValueAsString(snapshot);
    }

    @Override
    public ConfigurationSessionView approveOriginal(UUID configurationId) {
        return revalidateAndReprice(getEntity(configurationId));
    }

    @Override
    public ConfigurationSessionView approveWithPatch(UUID configurationId, ConfigurationPatch patch) {
        ConfigurationSession session = getEntity(configurationId);
        List<SelectionSnapshot.Extra> patchExtras = patch.extras().stream()
            .map(e -> new SelectionSnapshot.Extra(e.ingredientCode(), e.quantity()))
            .toList();
        session.updateSelections(
            session.getPizzaId(), patch.sizeCode(), patch.doughCode(),
            serialize(new SelectionSnapshot(patch.removedIngredientCodes(), patchExtras)), session.getComment());
        return revalidateAndReprice(session);
    }

    @Override
    public ConfigurationSessionView reject(UUID configurationId) {
        ConfigurationSession session = getEntity(configurationId);
        session.markValidated(ValidationStatus.REVIEW_REJECTED, null);
        return toView(session);
    }

    /**
     * Agent.md §4.3 steps 6-7: on a kitchen Accept or a customer accepting a
     * recommendation, the final configuration is revalidated *and* repriced
     * in one step — the customer only needs to see the final price and
     * confirm (step 8), no separate {@code /price} call required.
     */
    private ConfigurationSessionView revalidateAndReprice(ConfigurationSession session) {
        SelectionSnapshot snapshot = deserialize(session.getConfigurationJson());
        ConfigurationCandidate candidate = new ConfigurationCandidate(
            session.getPizzaId(),
            session.getSizeCode(),
            session.getDoughCode(),
            snapshot.removedIngredientCodes(),
            snapshot.extras().stream().map(e -> new ExtraSelection(e.ingredientCode(), e.quantity())).toList());

        ValidationResult result = ruleValidation.validate(candidate);
        boolean isValid = result.status() == com.example.pizzaconfigurator.rules.api.ValidationStatus.VALID;
        session.markValidated(isValid ? ValidationStatus.REVIEW_APPROVED : ValidationStatus.INVALID, result.ruleVersion());

        if (isValid) {
            PizzaView pizza = catalogQuery.getPizza(session.getPizzaId());
            List<PricedExtra> pricedExtras = snapshot.extras().stream()
                .map(e -> new PricedExtra(e.ingredientCode(), e.quantity()))
                .toList();
            ValidatedConfiguration validatedConfiguration = new ValidatedConfiguration(
                pizza.code(), session.getSizeCode(), session.getDoughCode(), pricedExtras, result.ruleVersion());
            PriceQuote quote = pricingService.calculate(validatedConfiguration);
            session.markPriced(quote.total(), quote.currency(), quote.priceVersion());
        }

        return toView(session);
    }

    private Set<String> union(Set<String> uiCodes, List<String> aiCodes) {
        Set<String> merged = new LinkedHashSet<>(uiCodes);
        merged.addAll(aiCodes);
        return merged;
    }

    private List<SelectionSnapshot.Extra> mergeExtras(List<SelectionSnapshot.Extra> uiExtras, List<ParsedExtra> aiExtras) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        uiExtras.forEach(e -> merged.put(e.ingredientCode(), e.quantity()));
        aiExtras.forEach(e -> merged.put(e.ingredientCode(), e.quantity()));
        return merged.entrySet().stream().map(e -> new SelectionSnapshot.Extra(e.getKey(), e.getValue())).toList();
    }

    public PricingOutcome priceSession(UUID configurationId) {
        ConfigurationSession session = getEntity(configurationId);
        requireNotExpired(session);

        boolean validated = session.getValidationStatus() == ValidationStatus.VALID
            || session.getValidationStatus() == ValidationStatus.REVIEW_APPROVED;
        if (!validated || session.getRuleVersion() == null) {
            throw new ConfigurationNotValidException();
        }

        PizzaView pizza = catalogQuery.getPizza(session.getPizzaId());
        SelectionSnapshot snapshot = deserialize(session.getConfigurationJson());
        List<PricedExtra> extras = snapshot.extras().stream()
            .map(e -> new PricedExtra(e.ingredientCode(), e.quantity()))
            .toList();

        ValidatedConfiguration validatedConfiguration = new ValidatedConfiguration(
            pizza.code(), session.getSizeCode(), session.getDoughCode(), extras, session.getRuleVersion());
        PriceQuote quote = pricingService.calculate(validatedConfiguration);

        session.markPriced(quote.total(), quote.currency(), quote.priceVersion());

        return new PricingOutcome(toView(session), quote);
    }

    private void requireNotExpired(ConfigurationSession session) {
        if (session.isExpired(Instant.now(clock))) {
            throw new ConfigurationExpiredException();
        }
    }

    private ConfigurationSession getEntity(UUID configurationId) {
        return sessions.findById(configurationId).orElseThrow(() -> new ConfigurationNotFoundException(configurationId));
    }

    private String serialize(SelectionSnapshot snapshot) {
        return jsonMapper.writeValueAsString(snapshot);
    }

    private SelectionSnapshot deserialize(String json) {
        return jsonMapper.readValue(json, SelectionSnapshot.class);
    }

    private ConfigurationSessionView toView(ConfigurationSession session) {
        SelectionSnapshot snapshot = deserialize(session.getConfigurationJson());
        return new ConfigurationSessionView(
            session.getConfigurationId(),
            session.getCustomerId(),
            session.getPizzaId(),
            session.getSizeCode(),
            session.getDoughCode(),
            snapshot.removedIngredientCodes(),
            snapshot.extras().stream().map(e -> new SelectionExtra(e.ingredientCode(), e.quantity())).toList(),
            session.getComment(),
            session.getValidationStatus(),
            session.getRuleVersion(),
            session.getPriceStatus(),
            session.getCalculatedPrice(),
            session.getPriceVersion(),
            session.getCurrency(),
            session.getExpiresAt());
    }
}
