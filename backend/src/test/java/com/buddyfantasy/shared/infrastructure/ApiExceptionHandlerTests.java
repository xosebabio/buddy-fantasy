package com.buddyfantasy.shared.infrastructure;

import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

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
}
