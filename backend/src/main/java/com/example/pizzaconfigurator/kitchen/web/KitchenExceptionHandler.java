package com.example.pizzaconfigurator.kitchen.web;

import com.example.pizzaconfigurator.orders.api.OrderNotFoundException;
import com.example.pizzaconfigurator.orders.api.OrderStateConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.kitchen.web")
class KitchenExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleOrderNotFound(OrderNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Order not found");
        return problem;
    }

    @ExceptionHandler(OrderStateConflictException.class)
    ProblemDetail handleOrderStateConflict(OrderStateConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("ORDER_STATE_CONFLICT");
        return problem;
    }
}
