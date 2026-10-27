package com.buddyfantasy.shared.infrastructure;

import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTests {

    enum TestError implements ErrorCode {
        SOME_REASON
    }

    final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void businessErrorsWithACodeExposeIt() {
        ProblemDetail invalid = handler.handleDomainValidation(
                new DomainValidationException(TestError.SOME_REASON, "Invalid"));
        ProblemDetail conflict = handler.handleConflict(new ConflictException(TestError.SOME_REASON, "Conflict"));

        assertThat(invalid.getStatus()).isEqualTo(400);
        assertThat(invalid.getProperties()).containsEntry("code", "SOME_REASON");
        assertThat(conflict.getStatus()).isEqualTo(409);
        assertThat(conflict.getProperties()).containsEntry("code", "SOME_REASON");
    }

    @Test
    void businessErrorsWithoutACodeHaveNoCodeProperty() {
        ProblemDetail invalid = handler.handleDomainValidation(new DomainValidationException("Invalid"));
        ProblemDetail conflict = handler.handleConflict(new ConflictException("Conflict"));

        assertThat(invalid.getProperties()).isNull();
        assertThat(conflict.getProperties()).isNull();
    }

    @Test
    void accessDeniedIs403WithoutItsReason() {
        ProblemDetail forbidden = handler.handleAccessDenied(new AccessDeniedException("Not an admin of league 42"));

        assertThat(forbidden.getStatus()).isEqualTo(403);
        assertThat(forbidden.getDetail()).isEqualTo("Forbidden");
    }

    @Test
    void unexpectedErrorsAre500WithoutTheirMessage() {
        ProblemDetail error = handler.handleUnexpected(new IllegalStateException("secret internals"));

        assertThat(error.getStatus()).isEqualTo(500);
        assertThat(error.getDetail()).isEqualTo("Internal error");
        assertThat(error.getProperties()).isNull();
    }
}
