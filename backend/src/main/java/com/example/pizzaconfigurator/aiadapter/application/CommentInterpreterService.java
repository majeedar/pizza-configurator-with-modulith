package com.example.pizzaconfigurator.aiadapter.application;

import com.example.pizzaconfigurator.aiadapter.api.CommentInterpretationRequest;
import com.example.pizzaconfigurator.aiadapter.api.CommentInterpreter;
import com.example.pizzaconfigurator.aiadapter.api.ParsedComment;
import com.example.pizzaconfigurator.aiadapter.api.ParsedExtra;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Agent.md §7.4: tries Deepseek first; on timeout, error, or malformed
 * response, fails over to OpenAI; if both fail, falls back to
 * {@code MANUAL_REVIEW_REQUIRED} (represented as {@link
 * ParsedComment#providerFailure()} — the caller, {@code
 * ConfigurationService}, maps a non-empty {@code unresolvedText} onto
 * {@code ValidationStatus.PENDING_REVIEW}). Also owns §13.2 steps 1-3:
 * schema validation happened at deserialization, this normalizes every
 * code against the pizza's actual allowed options and rejects anything
 * else as unresolved — step 4 (merge with the customer's structured UI
 * selections) and step 5 (send to the Rule Module) are the caller's job,
 * since this module must never decide validity.
 */
@Service
class CommentInterpreterService implements CommentInterpreter {

    private static final Logger log = LoggerFactory.getLogger(CommentInterpreterService.class);

    private final List<AiProviderClient> providersInOrder;
    private final CatalogQuery catalogQuery;

    CommentInterpreterService(DeepseekClient deepseekClient, OpenAiClient openAiClient, CatalogQuery catalogQuery) {
        this.providersInOrder = List.of(deepseekClient, openAiClient);
        this.catalogQuery = catalogQuery;
    }

    @Override
    public ParsedComment interpret(CommentInterpretationRequest request) {
        ConfigurableOptions options = catalogQuery.getOptions(request.pizzaId());

        for (AiProviderClient provider : providersInOrder) {
            try {
                RawParsedComment raw = provider.interpret(request.comment(), options);
                return normalize(raw, options);
            } catch (AiProviderException e) {
                log.warn("{} could not interpret comment, trying next provider: {}", provider.name(), e.getMessage());
            }
        }
        return ParsedComment.providerFailure();
    }

    private ParsedComment normalize(RawParsedComment raw, ConfigurableOptions options) {
        Set<String> removableCodes = options.baseIngredients().stream()
            .filter(i -> i.removable())
            .map(i -> i.ingredientCode())
            .collect(Collectors.toSet());
        Set<String> extraCodes = options.availableExtras().stream()
            .map(i -> i.code())
            .collect(Collectors.toSet());
        Set<String> sizeCodes = options.sizes().stream().map(s -> s.code()).collect(Collectors.toSet());
        Set<String> doughCodes = options.doughs().stream().map(d -> d.code()).collect(Collectors.toSet());

        List<String> unresolved = new ArrayList<>(raw.unresolvedText());

        List<String> removeIngredientCodes = new ArrayList<>();
        for (String code : raw.removeIngredients()) {
            if (removableCodes.contains(code)) {
                removeIngredientCodes.add(code);
            } else {
                unresolved.add("remove:" + code);
            }
        }

        List<ParsedExtra> extras = new ArrayList<>();
        for (RawExtra extra : raw.extras()) {
            if (extraCodes.contains(extra.ingredientCode()) && extra.quantity() >= 0) {
                extras.add(new ParsedExtra(extra.ingredientCode(), extra.quantity()));
            } else {
                unresolved.add("extra:" + extra.ingredientCode());
            }
        }

        String requestedSize = null;
        if (raw.requestedSize() != null) {
            if (sizeCodes.contains(raw.requestedSize())) {
                requestedSize = raw.requestedSize();
            } else {
                unresolved.add("size:" + raw.requestedSize());
            }
        }

        String requestedDough = null;
        if (raw.requestedDough() != null) {
            if (doughCodes.contains(raw.requestedDough())) {
                requestedDough = raw.requestedDough();
            } else {
                unresolved.add("dough:" + raw.requestedDough());
            }
        }

        return new ParsedComment(removeIngredientCodes, extras, requestedSize, requestedDough, List.copyOf(unresolved));
    }
}
