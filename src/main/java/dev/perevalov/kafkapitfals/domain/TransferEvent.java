package dev.perevalov.kafkapitfals.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Integration event describing one money transfer.
 *
 * @param eventId unique identifier used for partitioning and deduplication
 * @param fromAccountId sender account id
 * @param toAccountId recipient account id
 * @param amount transfer amount in precise decimal form
 * @param occurredAt event creation timestamp
 */
public record TransferEvent(
        String eventId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        Instant occurredAt
) {
}
