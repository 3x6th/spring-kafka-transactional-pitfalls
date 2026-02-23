package dev.perevalov.kafkapitfals.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request payload for outbox-based transfer endpoint.
 *
 * <p>Fix over bad request model: amount is {@link BigDecimal} to avoid floating-point rounding
 * errors in financial calculations.</p>
 *
 * @param fromAccountId sender account id
 * @param toAccountId recipient account id
 * @param amount transfer amount
 * @param failAfterDbWrite test flag that simulates crash before transaction commit
 */
public record OutboxTransferRequest(
        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,
        @NotNull @Positive BigDecimal amount,
        boolean failAfterDbWrite
) {
}
