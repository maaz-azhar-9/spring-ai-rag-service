package com.maazazhar.ragservice.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.maazazhar.ragservice.llm.LlmUnavailableException;

/** Maps domain errors to RFC 9457 problem details. Validation errors are handled by the parent. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(LlmUnavailableException.class)
    ProblemDetail llmUnavailable(LlmUnavailableException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Model unavailable", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
