package com.buddyfantasy.shared.infrastructure;

import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.shared.domain.ErrorCode;
import com.buddyfantasy.shared.domain.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps errors to RFC 9457 problem details. Malformed requests (unreadable JSON, missing body, wrong type in the path,
 * unsupported method…) are mapped by {@link ResponseEntityExceptionHandler}; anything unexpected is a 500 that hides
 * its message and stack trace from the caller and logs them.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleValidation(ConstraintViolationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("violations", e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleDomainValidation(DomainValidationException e) {
        return withCode(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()), e.code());
    }

    /** Generic body on purpose: callers must not learn why the resource is missing. */
    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Not found");
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException e) {
        return withCode(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()), e.code());
    }

    /** Thrown by the per-league checks of the handlers; without this, {@link #handleUnexpected} would make it a 500. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Forbidden");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    }

    private static ProblemDetail withCode(ProblemDetail problem, ErrorCode code) {
        if (code != null) {
            problem.setProperty("code", code.name());
        }
        return problem;
    }
}
