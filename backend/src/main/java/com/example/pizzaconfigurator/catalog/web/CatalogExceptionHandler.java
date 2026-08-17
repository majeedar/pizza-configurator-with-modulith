package com.example.pizzaconfigurator.catalog.web;

import com.example.pizzaconfigurator.catalog.api.PizzaNotFoundException;
import com.example.pizzaconfigurator.catalog.application.CatalogAdminNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.catalog.web")
class CatalogExceptionHandler {

    @ExceptionHandler(PizzaNotFoundException.class)
    ProblemDetail handlePizzaNotFound(PizzaNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Pizza not found");
        return problem;
    }

    @ExceptionHandler(CatalogAdminNotFoundException.class)
    ProblemDetail handleAdminNotFound(CatalogAdminNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Catalog entity not found");
        return problem;
    }

    /**
     * Catches, at minimum, the {@code pizza_ingredient_pizza_id_ingredient_id_key}
     * unique-constraint violation from adding the same ingredient to a pizza's
     * recipe twice — found via real Admin Portal testing, where it previously
     * surfaced as a raw 500 instead of a clean, actionable conflict response.
     * The underlying exception's message includes raw SQL/constraint names, so
     * it's deliberately not echoed back to the client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "This change conflicts with an existing catalog entry (e.g. that ingredient is already on this pizza's recipe)."
        );
        problem.setTitle("Conflict");
        return problem;
    }
}
