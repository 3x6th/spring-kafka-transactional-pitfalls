package dev.perevalov.kafkapitfals.api;

import dev.perevalov.kafkapitfals.domain.OutboxStatus;

import java.time.Instant;

/**
 * Read model for outbox row state.
 *
 * @param id outbox event id
 * @param status current outbox status
 * @param createdAt creation timestamp
 * @param sentAt send timestamp, {@code null} for pending rows
 */
public record OutboxEventView(
        String id,
        OutboxStatus status,
        Instant createdAt,
        Instant sentAt
) {
}
