package io.rxbridge.enrollment.common.web;

import io.rxbridge.enrollment.common.error.ConflictException;
import io.rxbridge.enrollment.common.error.DomainValidationException;
import io.rxbridge.enrollment.common.error.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String RECORD_CHANGED_ANOTHER = "The record was changed by another request";
    private static final String UNEXPECTED_ERROR = "Unexpected error";

    public record FieldProblem(String field, String message) {}

    @ExceptionHandler
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail conflict(ConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail invalid(DomainValidationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail staleVersion(OptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, RECORD_CHANGED_ANOTHER);
    }

    @ExceptionHandler
    ProblemDetail unexpected(Exception e) {
        log.error(UNEXPECTED_ERROR, e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail problem = ex.getBody();
        List<FieldProblem> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(ApiExceptionHandler::toFieldProblem)
            .toList();
        problem.setProperty("errors", errors);

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
        HandlerMethodValidationException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail problem = ex.getBody();
        List<FieldProblem> errors = new ArrayList<>();
        for (ParameterErrors body : ex.getBeanResults()) {
            body.getFieldErrors().stream()
                .map(ApiExceptionHandler::toFieldProblem)
                .forEach(errors::add);
        }

        for (ParameterValidationResult value : ex.getValueResults()) {
            String parameter = value.getMethodParameter().getParameterName();
            value.getResolvableErrors().stream()
                .map(error -> new FieldProblem(parameter, error.getDefaultMessage()))
                .forEach(errors::add);
        }
        problem.setProperty("errors", errors);

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static FieldProblem toFieldProblem(FieldError error) {
        return new FieldProblem(error.getField(), error.getDefaultMessage());
    }
}
