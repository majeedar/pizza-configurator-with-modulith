package com.example.pizzaconfigurator.catalog.api;

public record RecipeItemView(
    String ingredientCode,
    String ingredientName,
    int defaultQuantity,
    boolean removable
) {
}
