package com.example.pizzaconfigurator.security.web;

import com.example.pizzaconfigurator.security.api.EmailAlreadyRegisteredException;
import com.example.pizzaconfigurator.security.api.EmployeeNotFoundException;
import com.example.pizzaconfigurator.security.api.InvalidCredentialsException;
import com.example.pizzaconfigurator.security.api.InvalidStaffCredentialsException;
import com.example.pizzaconfigurator.security.api.UsernameAlreadyRegisteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.security.web")
class SecurityExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Email already registered");
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Invalid credentials");
        return problem;
    }

    @ExceptionHandler(InvalidStaffCredentialsException.class)
    ProblemDetail handleInvalidStaffCredentials(InvalidStaffCredentialsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Invalid credentials");
        return problem;
    }

    @ExceptionHandler(UsernameAlreadyRegisteredException.class)
    ProblemDetail handleUsernameAlreadyRegistered(UsernameAlreadyRegisteredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Username already registered");
        return problem;
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    ProblemDetail handleEmployeeNotFound(EmployeeNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Employee not found");
        return problem;
    }
}
