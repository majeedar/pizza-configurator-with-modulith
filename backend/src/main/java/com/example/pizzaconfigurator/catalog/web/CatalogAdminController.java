package com.example.pizzaconfigurator.catalog.web;

import com.example.pizzaconfigurator.catalog.application.CatalogAdminService;
import com.example.pizzaconfigurator.catalog.domain.Dough;
import com.example.pizzaconfigurator.catalog.domain.Ingredient;
import com.example.pizzaconfigurator.catalog.domain.Pizza;
import com.example.pizzaconfigurator.catalog.domain.PizzaIngredient;
import com.example.pizzaconfigurator.catalog.domain.Size;
import com.example.pizzaconfigurator.catalog.web.dto.DoughAdminRequest;
import com.example.pizzaconfigurator.catalog.web.dto.DoughAdminResponse;
import com.example.pizzaconfigurator.catalog.web.dto.IngredientAdminRequest;
import com.example.pizzaconfigurator.catalog.web.dto.IngredientAdminResponse;
import com.example.pizzaconfigurator.catalog.web.dto.PizzaAdminRequest;
import com.example.pizzaconfigurator.catalog.web.dto.PizzaAdminResponse;
import com.example.pizzaconfigurator.catalog.web.dto.RecipeLineAdminRequest;
import com.example.pizzaconfigurator.catalog.web.dto.RecipeLineAdminResponse;
import com.example.pizzaconfigurator.catalog.web.dto.SizeAdminRequest;
import com.example.pizzaconfigurator.catalog.web.dto.SizeAdminResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin CRUD for catalog resources (agent.md §7.10, §9.3) — requires
 * {@code ROLE_ADMIN} via the {@code /api/v1/admin/**} Spring Security
 * filter-chain rule (agent.md §14.2, Phase 7).
 */
@RestController
@RequestMapping("/api/v1/admin")
class CatalogAdminController {

    private final CatalogAdminService adminService;

    CatalogAdminController(CatalogAdminService adminService) {
        this.adminService = adminService;
    }

    // --- Pizzas ---

    @GetMapping("/pizzas")
    List<PizzaAdminResponse> findAllPizzas() {
        return adminService.findAllPizzas().stream().map(this::toResponse).toList();
    }

    @GetMapping("/pizzas/{pizzaId}")
    PizzaAdminResponse getPizza(@PathVariable UUID pizzaId) {
        return toResponse(adminService.getPizza(pizzaId));
    }

    @PostMapping("/pizzas")
    ResponseEntity<PizzaAdminResponse> createPizza(@Valid @RequestBody PizzaAdminRequest request) {
        Pizza pizza = adminService.createPizza(
            request.code(), request.name(), request.description(), request.basePrice(), request.active());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(pizza));
    }

    @PutMapping("/pizzas/{pizzaId}")
    PizzaAdminResponse updatePizza(@PathVariable UUID pizzaId, @Valid @RequestBody PizzaAdminRequest request) {
        Pizza pizza = adminService.updatePizza(
            pizzaId, request.name(), request.description(), request.basePrice(), request.active());
        return toResponse(pizza);
    }

    @PostMapping(value = "/pizzas/{pizzaId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    PizzaAdminResponse uploadPizzaImage(@PathVariable UUID pizzaId, @RequestParam("file") MultipartFile file) {
        try {
            Pizza pizza = adminService.updatePizzaImage(pizzaId, file.getContentType(), file.getBytes());
            return toResponse(pizza);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // --- Ingredients ---

    @GetMapping("/ingredients")
    List<IngredientAdminResponse> findAllIngredients() {
        return adminService.findAllIngredients().stream().map(this::toResponse).toList();
    }

    @PostMapping("/ingredients")
    ResponseEntity<IngredientAdminResponse> createIngredient(@Valid @RequestBody IngredientAdminRequest request) {
        Ingredient ingredient = adminService.createIngredient(
            request.code(), request.name(), request.type(), request.active(), request.defaultUnit());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(ingredient));
    }

    @PutMapping("/ingredients/{ingredientId}")
    IngredientAdminResponse updateIngredient(
        @PathVariable UUID ingredientId, @Valid @RequestBody IngredientAdminRequest request
    ) {
        Ingredient ingredient = adminService.updateIngredient(
            ingredientId, request.name(), request.type(), request.active(), request.defaultUnit());
        return toResponse(ingredient);
    }

    // --- Sizes ---

    @GetMapping("/sizes")
    List<SizeAdminResponse> findAllSizes() {
        return adminService.findAllSizes().stream().map(this::toResponse).toList();
    }

    @PostMapping("/sizes")
    ResponseEntity<SizeAdminResponse> createSize(@Valid @RequestBody SizeAdminRequest request) {
        Size size = adminService.createSize(request.code(), request.displayName(), request.priceModifier(), request.active());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(size));
    }

    @PutMapping("/sizes/{sizeId}")
    SizeAdminResponse updateSize(@PathVariable UUID sizeId, @Valid @RequestBody SizeAdminRequest request) {
        Size size = adminService.updateSize(sizeId, request.displayName(), request.priceModifier(), request.active());
        return toResponse(size);
    }

    // --- Doughs ---

    @GetMapping("/doughs")
    List<DoughAdminResponse> findAllDoughs() {
        return adminService.findAllDoughs().stream().map(this::toResponse).toList();
    }

    @PostMapping("/doughs")
    ResponseEntity<DoughAdminResponse> createDough(@Valid @RequestBody DoughAdminRequest request) {
        Dough dough = adminService.createDough(request.code(), request.displayName(), request.priceModifier(), request.active());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(dough));
    }

    @PutMapping("/doughs/{doughId}")
    DoughAdminResponse updateDough(@PathVariable UUID doughId, @Valid @RequestBody DoughAdminRequest request) {
        Dough dough = adminService.updateDough(doughId, request.displayName(), request.priceModifier(), request.active());
        return toResponse(dough);
    }

    // --- Recipe ---

    @GetMapping("/pizzas/{pizzaId}/recipe")
    List<RecipeLineAdminResponse> getRecipe(@PathVariable UUID pizzaId) {
        return adminService.getRecipe(pizzaId).stream().map(this::toResponse).toList();
    }

    @PostMapping("/pizzas/{pizzaId}/recipe")
    ResponseEntity<RecipeLineAdminResponse> addRecipeLine(
        @PathVariable UUID pizzaId, @Valid @RequestBody RecipeLineAdminRequest request
    ) {
        PizzaIngredient line = adminService.addRecipeLine(
            pizzaId, request.ingredientCode(), request.defaultQuantity(), request.removable());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(line));
    }

    @PutMapping("/pizzas/{pizzaId}/recipe/{pizzaIngredientId}")
    RecipeLineAdminResponse updateRecipeLine(
        @PathVariable UUID pizzaId, @PathVariable UUID pizzaIngredientId, @Valid @RequestBody RecipeLineAdminRequest request
    ) {
        PizzaIngredient line = adminService.updateRecipeLine(
            pizzaIngredientId, request.defaultQuantity(), request.removable());
        return toResponse(line);
    }

    @DeleteMapping("/pizzas/{pizzaId}/recipe/{pizzaIngredientId}")
    ResponseEntity<Void> removeRecipeLine(@PathVariable UUID pizzaId, @PathVariable UUID pizzaIngredientId) {
        adminService.removeRecipeLine(pizzaIngredientId);
        return ResponseEntity.noContent().build();
    }

    // --- Mapping ---

    private PizzaAdminResponse toResponse(Pizza pizza) {
        return new PizzaAdminResponse(
            pizza.getPizzaId(), pizza.getCode(), pizza.getName(), pizza.getDescription(),
            pizza.getBasePrice(), pizza.isActive(), pizza.getImageUrl(), pizza.getVersion());
    }

    private IngredientAdminResponse toResponse(Ingredient ingredient) {
        return new IngredientAdminResponse(
            ingredient.getIngredientId(), ingredient.getCode(), ingredient.getName(),
            ingredient.getType(), ingredient.isActive(), ingredient.getDefaultUnit());
    }

    private SizeAdminResponse toResponse(Size size) {
        return new SizeAdminResponse(
            size.getSizeId(), size.getCode(), size.getDisplayName(), size.getPriceModifier(), size.isActive());
    }

    private DoughAdminResponse toResponse(Dough dough) {
        return new DoughAdminResponse(
            dough.getDoughId(), dough.getCode(), dough.getDisplayName(), dough.getPriceModifier(), dough.isActive());
    }

    private RecipeLineAdminResponse toResponse(PizzaIngredient line) {
        return new RecipeLineAdminResponse(
            line.getPizzaIngredientId(), line.getIngredient().getCode(), line.getIngredient().getName(),
            line.getDefaultQuantity(), line.isRemovable());
    }
}
