package com.example.pizzaconfigurator.catalog.application;

import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;
import com.example.pizzaconfigurator.catalog.api.DoughOptionView;
import com.example.pizzaconfigurator.catalog.api.IngredientOptionView;
import com.example.pizzaconfigurator.catalog.api.PizzaNotFoundException;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import com.example.pizzaconfigurator.catalog.api.PizzaView;
import com.example.pizzaconfigurator.catalog.api.RecipeItemView;
import com.example.pizzaconfigurator.catalog.api.SizeOptionView;
import com.example.pizzaconfigurator.catalog.domain.Pizza;
import com.example.pizzaconfigurator.catalog.domain.PizzaIngredient;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.DoughRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.IngredientRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.PizzaIngredientRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.PizzaRepository;
import com.example.pizzaconfigurator.catalog.infrastructure.persistence.SizeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class CatalogQueryService implements CatalogQuery {

    private final PizzaRepository pizzas;
    private final PizzaIngredientRepository recipeLines;
    private final IngredientRepository ingredients;
    private final SizeRepository sizes;
    private final DoughRepository doughs;

    CatalogQueryService(
        PizzaRepository pizzas,
        PizzaIngredientRepository recipeLines,
        IngredientRepository ingredients,
        SizeRepository sizes,
        DoughRepository doughs
    ) {
        this.pizzas = pizzas;
        this.recipeLines = recipeLines;
        this.ingredients = ingredients;
        this.sizes = sizes;
        this.doughs = doughs;
    }

    @Override
    public PizzaView getPizza(UUID pizzaId) {
        Pizza pizza = pizzas.findById(pizzaId).orElseThrow(() -> new PizzaNotFoundException(pizzaId));
        return toView(pizza);
    }

    @Override
    public List<PizzaSummary> findActivePizzas() {
        return pizzas.findByActiveTrue().stream()
            .map(p -> new PizzaSummary(p.getPizzaId(), p.getCode(), p.getName(), p.getDescription(), p.getBasePrice()))
            .toList();
    }

    @Override
    public ConfigurableOptions getOptions(UUID pizzaId) {
        Pizza pizza = pizzas.findById(pizzaId).orElseThrow(() -> new PizzaNotFoundException(pizzaId));
        List<RecipeItemView> recipe = recipe(pizza);

        List<IngredientOptionView> extras = ingredients.findByActiveTrue().stream()
            .map(i -> new IngredientOptionView(i.getCode(), i.getName(), i.getType()))
            .toList();

        List<SizeOptionView> sizeOptions = sizes.findByActiveTrue().stream()
            .map(s -> new SizeOptionView(s.getCode(), s.getDisplayName(), s.getPriceModifier()))
            .toList();

        List<DoughOptionView> doughOptions = doughs.findByActiveTrue().stream()
            .map(d -> new DoughOptionView(d.getCode(), d.getDisplayName(), d.getPriceModifier()))
            .toList();

        return new ConfigurableOptions(pizzaId, recipe, extras, sizeOptions, doughOptions);
    }

    private PizzaView toView(Pizza pizza) {
        return new PizzaView(
            pizza.getPizzaId(),
            pizza.getCode(),
            pizza.getName(),
            pizza.getDescription(),
            pizza.getBasePrice(),
            recipe(pizza)
        );
    }

    private List<RecipeItemView> recipe(Pizza pizza) {
        return recipeLines.findByPizza_PizzaId(pizza.getPizzaId()).stream()
            .map(this::toRecipeItemView)
            .toList();
    }

    private RecipeItemView toRecipeItemView(PizzaIngredient line) {
        return new RecipeItemView(
            line.getIngredient().getCode(),
            line.getIngredient().getName(),
            line.getDefaultQuantity(),
            line.isRemovable()
        );
    }
}
