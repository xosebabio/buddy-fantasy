package com.buddyfantasy.shared.infrastructure;

import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.shared.domain.ErrorCode;
import com.buddyfantasy.shared.domain.ForbiddenException;
import com.buddyfantasy.shared.domain.UnexpectedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiExceptionHandlerTests {

    enum TestError implements ErrorCode {
        SOME_REASON
    }

    final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    @DisplayName("Business errors with a code expose it")
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
    @DisplayName("Validation errors without a code have no code property")
    void validationErrorsWithoutACodeHaveNoCodeProperty() {
        ProblemDetail invalid = handler.handleDomainValidation(new DomainValidationException("Invalid"));

        assertThat(invalid.getProperties()).isNull();
    }

    @Test
    @DisplayName("A conflict always has a code")
    void aConflictAlwaysHasACode() {
        assertThatThrownBy(() -> new ConflictException(null, "Conflict")).isInstanceOf(UnexpectedException.class);
    }

    @Test
    @DisplayName("Forbidden is 403 without its reason")
    void forbiddenIs403WithoutItsReason() {
        ProblemDetail forbidden = handler.handleForbidden(new ForbiddenException("Not an admin of league 42"));

        assertThat(forbidden.getStatus()).isEqualTo(403);
        assertThat(forbidden.getDetail()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("Unexpected errors are 500 without their message")
    void unexpectedErrorsAre500WithoutTheirMessage() {
        ProblemDetail error = handler.handleUnexpected(new UnexpectedException("secret internals"));

        assertThat(error.getStatus()).isEqualTo(500);
        assertThat(error.getDetail()).isEqualTo("Internal error");
        assertThat(error.getProperties()).isNull();
    }
}
