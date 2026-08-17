package com.example.pizzaconfigurator.recommendation.web;

import com.example.pizzaconfigurator.recommendation.api.IllegalReviewStateException;
import com.example.pizzaconfigurator.recommendation.api.NoPendingRecommendationException;
import com.example.pizzaconfigurator.recommendation.api.ReviewRequestNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.pizzaconfigurator.recommendation.web")
class RecommendationExceptionHandler {

    @ExceptionHandler(ReviewRequestNotFoundException.class)
    ProblemDetail handleNotFound(ReviewRequestNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Review request not found");
        return problem;
    }

    @ExceptionHandler(NoPendingRecommendationException.class)
    ProblemDetail handleNoPendingRecommendation(NoPendingRecommendationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("No pending recommendation");
        return problem;
    }

    @ExceptionHandler(IllegalReviewStateException.class)
    ProblemDetail handleIllegalState(IllegalReviewStateException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("REVIEW_STATE_CONFLICT");
        return problem;
    }
}
