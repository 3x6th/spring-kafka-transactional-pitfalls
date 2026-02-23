package dev.perevalov.kafkapitfals.api;

import java.util.List;

/**
 * Aggregated snapshot of all stores used in the laboratory.
 *
 * @param accounts current account balances
 * @param outbox outbox events with current statuses
 * @param outboxNew number of pending outbox rows
 * @param outboxSent number of already sent outbox rows
 * @param naiveLedgerRows rows created by non-idempotent consumer
 * @param idempotentLedgerRows rows created by idempotent consumer
 * @param processedEvents dedup marker count
 */
public record LabStateResponse(
        List<AccountView> accounts,
        List<OutboxEventView> outbox,
        long outboxNew,
        long outboxSent,
        long naiveLedgerRows,
        long idempotentLedgerRows,
        long processedEvents
) {
}
