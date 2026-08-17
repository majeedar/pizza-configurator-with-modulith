package com.example.pizzaconfigurator.catalog.application;

import com.example.pizzaconfigurator.catalog.domain.Dough;
import com.example.pizzaconfigurator.catalog.domain.Ingredient;
import com.example.pizzaconfigurator.catalog.domain.IngredientType;
import com.example.pizzaconfigurator.catalog.domain.Pizza;
import com.example.pizzaconfigurator.catalog.domain.PizzaIngredient;
import com.example.pizzaconfigurator.catalog.domain.Size;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.DoughRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.IngredientRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.PizzaIngredientRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.PizzaRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.SizeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin write operations for catalog entities (agent.md §7.10, §9.3). Not a
 * published module API — the admin controller is this module's own concern
 * (see the RuleAdminController precedent in agent.md §23), same as every
 * other module owns its own admin endpoints.
 */
@Service
@Transactional
public class CatalogAdminService {

    private final PizzaRepository pizzas;
    private final IngredientRepository ingredients;
    private final SizeRepository sizes;
    private final DoughRepository doughs;
    private final PizzaIngredientRepository recipeLines;

    CatalogAdminService(
        PizzaRepository pizzas,
        IngredientRepository ingredients,
        SizeRepository sizes,
        DoughRepository doughs,
        PizzaIngredientRepository recipeLines
    ) {
        this.pizzas = pizzas;
        this.ingredients = ingredients;
        this.sizes = sizes;
        this.doughs = doughs;
        this.recipeLines = recipeLines;
    }

    // --- Pizza ---

    @Transactional(readOnly = true)
    public List<Pizza> findAllPizzas() {
        return pizzas.findAll();
    }

    @Transactional(readOnly = true)
    public Pizza getPizza(UUID pizzaId) {
        return pizzas.findById(pizzaId).orElseThrow(() -> new CatalogAdminNotFoundException("pizza", pizzaId));
    }

    public Pizza createPizza(String code, String name, String description, BigDecimal basePrice, boolean active) {
        return pizzas.save(new Pizza(code, name, description, basePrice, active));
    }

    public Pizza updatePizza(UUID pizzaId, String name, String description, BigDecimal basePrice, boolean active) {
        Pizza pizza = getPizza(pizzaId);
        pizza.update(name, description, basePrice, active);
        return pizza;
    }

    // --- Ingredient ---

    @Transactional(readOnly = true)
    public List<Ingredient> findAllIngredients() {
        return ingredients.findAll();
    }

    @Transactional(readOnly = true)
    public Ingredient getIngredient(UUID ingredientId) {
        return ingredients.findById(ingredientId)
            .orElseThrow(() -> new CatalogAdminNotFoundException("ingredient", ingredientId));
    }

    public Ingredient createIngredient(String code, String name, IngredientType type, boolean active, String defaultUnit) {
        return ingredients.save(new Ingredient(code, name, type, active, defaultUnit));
    }

    public Ingredient updateIngredient(UUID ingredientId, String name, IngredientType type, boolean active, String defaultUnit) {
        Ingredient ingredient = getIngredient(ingredientId);
        ingredient.update(name, type, active, defaultUnit);
        return ingredient;
    }

    // --- Size ---

    @Transactional(readOnly = true)
    public List<Size> findAllSizes() {
        return sizes.findAll();
    }

    public Size createSize(String code, String displayName, BigDecimal priceModifier, boolean active) {
        return sizes.save(new Size(code, displayName, priceModifier, active));
    }

    public Size updateSize(UUID sizeId, String displayName, BigDecimal priceModifier, boolean active) {
        Size size = sizes.findById(sizeId).orElseThrow(() -> new CatalogAdminNotFoundException("size", sizeId));
        size.update(displayName, priceModifier, active);
        return size;
    }

    // --- Dough ---

    @Transactional(readOnly = true)
    public List<Dough> findAllDoughs() {
        return doughs.findAll();
    }

    public Dough createDough(String code, String displayName, BigDecimal priceModifier, boolean active) {
        return doughs.save(new Dough(code, displayName, priceModifier, active));
    }

    public Dough updateDough(UUID doughId, String displayName, BigDecimal priceModifier, boolean active) {
        Dough dough = doughs.findById(doughId).orElseThrow(() -> new CatalogAdminNotFoundException("dough", doughId));
        dough.update(displayName, priceModifier, active);
        return dough;
    }

    // --- Recipe (PizzaIngredient) ---

    @Transactional(readOnly = true)
    public List<PizzaIngredient> getRecipe(UUID pizzaId) {
        return recipeLines.findByPizza_PizzaId(pizzaId);
    }

    public PizzaIngredient addRecipeLine(UUID pizzaId, String ingredientCode, int defaultQuantity, boolean removable) {
        Pizza pizza = getPizza(pizzaId);
        Ingredient ingredient = ingredients.findByCode(ingredientCode)
            .orElseThrow(() -> new CatalogAdminNotFoundException("ingredient", ingredientCode));
        return recipeLines.save(new PizzaIngredient(pizza, ingredient, defaultQuantity, removable));
    }

    public PizzaIngredient updateRecipeLine(UUID pizzaIngredientId, int defaultQuantity, boolean removable) {
        PizzaIngredient line = recipeLines.findWithIngredientById(pizzaIngredientId)
            .orElseThrow(() -> new CatalogAdminNotFoundException("recipe line", pizzaIngredientId));
        line.update(defaultQuantity, removable);
        return line;
    }

    public void removeRecipeLine(UUID pizzaIngredientId) {
        if (!recipeLines.existsById(pizzaIngredientId)) {
            throw new CatalogAdminNotFoundException("recipe line", pizzaIngredientId);
        }
        recipeLines.deleteById(pizzaIngredientId);
    }
}
