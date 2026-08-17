package com.example.pizzaconfigurator.admin.web;

import com.example.pizzaconfigurator.admin.api.AppLinkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.admin.web")
class AdminExceptionHandler {

    @ExceptionHandler(AppLinkNotFoundException.class)
    ProblemDetail handleAppLinkNotFound(AppLinkNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("App link not configured");
        return problem;
    }

    @ExceptionHandler(InvalidAudienceException.class)
    ProblemDetail handleInvalidAudience(InvalidAudienceException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid audience");
        return problem;
    }
}
