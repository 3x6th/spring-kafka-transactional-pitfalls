package dev.perevalov.kafkapitfals.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Centralized mapping of domain/service exceptions to stable API responses.
 *
 * <p>Problem addressed: without explicit mapping, clients receive framework-specific responses that
 * are harder to use in automated labs and scripts.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Maps argument validation or business precondition failures to HTTP 400.
     *
     * @param ex source exception
     * @return compact error payload for scripts and manual checks
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "error", "bad_request",
                "message", ex.getMessage()
        );
    }

    /**
     * Maps state conflict failures (simulated crash windows, relay conflicts) to HTTP 409.
     *
     * @param ex source exception
     * @return compact error payload for scripts and manual checks
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleIllegalState(IllegalStateException ex) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "error", "conflict",
                "message", ex.getMessage()
        );
    }
}
