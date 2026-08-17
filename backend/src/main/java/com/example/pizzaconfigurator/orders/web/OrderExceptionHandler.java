package com.example.pizzaconfigurator.orders.web;

import com.example.pizzaconfigurator.basket.api.BasketAlreadyCheckedOutException;
import com.example.pizzaconfigurator.basket.api.BasketEmptyException;
import com.example.pizzaconfigurator.basket.api.BasketNotFoundException;
import com.example.pizzaconfigurator.orders.api.OrderAccessDeniedException;
import com.example.pizzaconfigurator.orders.api.OrderNotFoundException;
import com.example.pizzaconfigurator.orders.api.OrderStateConflictException;
import com.example.pizzaconfigurator.orders.application.IdempotencyKeyConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.orders.web")
class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleOrderNotFound(OrderNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Order not found");
        return problem;
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    ProblemDetail handleAccessDenied(OrderAccessDeniedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Order access denied");
        return problem;
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Idempotency-Key conflict");
        return problem;
    }

    @ExceptionHandler(OrderStateConflictException.class)
    ProblemDetail handleOrderStateConflict(OrderStateConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("ORDER_STATE_CONFLICT");
        return problem;
    }

    @ExceptionHandler(BasketNotFoundException.class)
    ProblemDetail handleBasketNotFound(BasketNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Unknown basket");
        return problem;
    }

    @ExceptionHandler(BasketEmptyException.class)
    ProblemDetail handleBasketEmpty(BasketEmptyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Basket is empty");
        return problem;
    }

    @ExceptionHandler(BasketAlreadyCheckedOutException.class)
    ProblemDetail handleBasketAlreadyCheckedOut(BasketAlreadyCheckedOutException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Basket already checked out");
        return problem;
    }
}
