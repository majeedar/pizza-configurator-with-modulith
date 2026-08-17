package com.example.pizzaconfigurator.configuration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ConfigurationRequest(
    @NotNull UUID pizzaId,
    @NotBlank String sizeCode,
    @NotBlank String doughCode,
    Set<String> removedIngredients,
    List<ExtraRequest> extras,
    @Size(max = 1000) String comment
) {
    public Set<String> removedIngredientsOrEmpty() {
        return removedIngredients == null ? Set.of() : removedIngredients;
    }

    public List<ExtraRequest> extrasOrEmpty() {
        return extras == null ? List.of() : extras;
    }
}
