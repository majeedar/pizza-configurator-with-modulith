package com.example.pizzaconfigurator.pricing.web;

import com.example.pizzaconfigurator.pricing.application.PriceAdminNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.pricing.web")
class PriceExceptionHandler {

    @ExceptionHandler(PriceAdminNotFoundException.class)
    ProblemDetail handlePriceNotFound(PriceAdminNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Price not found");
        return problem;
    }
}
