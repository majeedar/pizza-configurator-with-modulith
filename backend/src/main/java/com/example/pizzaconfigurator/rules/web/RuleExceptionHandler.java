package com.example.pizzaconfigurator.rules.web;

import com.example.pizzaconfigurator.rules.application.RuleAdminNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.rules.web")
class RuleExceptionHandler {

    @ExceptionHandler(RuleAdminNotFoundException.class)
    ProblemDetail handleRuleNotFound(RuleAdminNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Rule not found");
        return problem;
    }
}
