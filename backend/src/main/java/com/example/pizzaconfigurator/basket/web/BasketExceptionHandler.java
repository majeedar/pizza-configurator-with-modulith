package com.example.pizzaconfigurator.basket.web;

import com.example.pizzaconfigurator.basket.api.BasketNotFoundException;
import com.example.pizzaconfigurator.basket.application.ConfigurationNotReadyException;
import com.example.pizzaconfigurator.configuration.api.ConfigurationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.basket.web")
class BasketExceptionHandler {

    @ExceptionHandler(BasketNotFoundException.class)
    ProblemDetail handleBasketNotFound(BasketNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Basket not found");
        return problem;
    }

    @ExceptionHandler(ConfigurationNotFoundException.class)
    ProblemDetail handleConfigurationNotFound(ConfigurationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Unknown configuration");
        return problem;
    }

    @ExceptionHandler(ConfigurationNotReadyException.class)
    ProblemDetail handleConfigurationNotReady(ConfigurationNotReadyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Configuration not ready for checkout");
        return problem;
    }
}
