package com.example.pizzaconfigurator.recommendation.web.dto;

import com.example.pizzaconfigurator.configuration.api.ConfigurationPatch;
import com.example.pizzaconfigurator.configuration.api.SelectionExtra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;

public record ConfigurationPatchRequest(
    Set<String> removedIngredientCodes,
    @Valid List<PatchExtraRequest> extras,
    @NotBlank String sizeCode,
    @NotBlank String doughCode
) {
    public ConfigurationPatch toConfigurationPatch() {
        Set<String> removed = removedIngredientCodes == null ? Set.of() : removedIngredientCodes;
        List<SelectionExtra> patchExtras = (extras == null ? List.<PatchExtraRequest>of() : extras).stream()
            .map(e -> new SelectionExtra(e.ingredientCode(), e.quantity()))
            .toList();
        return new ConfigurationPatch(removed, patchExtras, sizeCode, doughCode);
    }
}
