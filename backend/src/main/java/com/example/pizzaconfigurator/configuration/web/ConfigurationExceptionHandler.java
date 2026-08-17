package com.example.pizzaconfigurator.configuration.web;

import com.example.pizzaconfigurator.catalog.api.PizzaNotFoundException;
import com.example.pizzaconfigurator.configuration.api.ConfigurationNotFoundException;
import com.example.pizzaconfigurator.configuration.application.ConfigurationExpiredException;
import com.example.pizzaconfigurator.configuration.application.ConfigurationNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.configuration.web")
class ConfigurationExceptionHandler {

    @ExceptionHandler(ConfigurationNotFoundException.class)
    ProblemDetail handleNotFound(ConfigurationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Configuration not found");
        return problem;
    }

    @ExceptionHandler(PizzaNotFoundException.class)
    ProblemDetail handleUnknownPizza(PizzaNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Unknown pizza");
        return problem;
    }

    @ExceptionHandler(ConfigurationNotValidException.class)
    ProblemDetail handleNotValid(ConfigurationNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("CONFIGURATION_INVALID");
        return problem;
    }

    @ExceptionHandler(ConfigurationExpiredException.class)
    ProblemDetail handleExpired(ConfigurationExpiredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, exception.getMessage());
        problem.setTitle("CONFIGURATION_EXPIRED");
        return problem;
    }
}
