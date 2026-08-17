package com.example.pizzaconfigurator.aiadapter.application;

/** Mirrors the structured-output schema (agent.md §13.2) before catalog-code normalization. */
record RawExtra(String ingredientCode, int quantity) {
}
