package dev.perevalov.kafkapitfals.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload for unsafe dual-write transfer endpoint.
 *
 * <p>Problem intentionally preserved: amount uses {@link Double} to reflect common anti-pattern from
 * legacy service code and discuss precision risks for money.</p>
 *
 * @param fromAccountId sender account id
 * @param toAccountId recipient account id
 * @param amount transfer amount in floating-point form
 * @param failAfterKafkaSend test flag that simulates crash after Kafka publish
 */
public record BadTransferRequest(
        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,
        @NotNull @Positive Double amount,
        boolean failAfterKafkaSend
) {
}
